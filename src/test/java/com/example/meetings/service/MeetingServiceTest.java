package com.example.meetings.service;

import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import com.example.meetings.repository.MeetingParticipantRepository;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingParticipantRepository participantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MeetingService meetingService;

    private User organizer;

    @BeforeEach
    void setUp() {
        organizer = new User("tiago", "tiago@example.com", "password");
        org.springframework.test.util.ReflectionTestUtils.setField(organizer, "id", 1L);
    }

    @Test
    void propose_endBeforeStart_throwsException() {
        Instant start = Instant.now();
        Instant end = start.minusSeconds(3600);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            meetingService.propose(organizer, "Title", "Desc", start, end, List.of());
        });

        assertThat(ex.getMessage()).isEqualTo("End time must be after start time");
    }

    @Test
    void propose_validData_savesMeetingAndAutoAcceptsForOrganizer() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);

        when(meetingRepository.save(any(Meeting.class))).thenAnswer(i -> i.getArguments()[0]);

        Meeting meeting = meetingService.propose(organizer, "Title", "Desc", start, end, List.of());

        assertThat(meeting.getTitle()).isEqualTo("Title");
        assertThat(meeting.getOrganizer()).isEqualTo(organizer);
        assertThat(meeting.getParticipants()).hasSize(1);
        assertThat(meeting.getParticipants().iterator().next().getStatus()).isEqualTo(InviteStatus.ACCEPTED);

        verify(meetingRepository).save(meeting);
    }

    @Test
    void propose_withInvitees_addsPendingParticipants() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);
        User invitee1 = new User("moura", "moura@example.com", "pass");
        org.springframework.test.util.ReflectionTestUtils.setField(invitee1, "id", 2L);
        User invitee2 = new User("pkto", "pkto@example.com", "pass");
        org.springframework.test.util.ReflectionTestUtils.setField(invitee2, "id", 3L);

        when(userRepository.findByUsername("moura")).thenReturn(Optional.of(invitee1));
        when(userRepository.findByUsername("pkto")).thenReturn(Optional.of(invitee2));
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(i -> i.getArguments()[0]);

        Meeting meeting = meetingService.propose(organizer, "Title", "Desc", start, end,
                List.of("moura", "pkto", "moura", "   "));

        assertThat(meeting.getParticipants()).hasSize(3);

        long pendingCount = meeting.getParticipants().stream()
                .filter(p -> p.getStatus() == InviteStatus.PENDING)
                .count();
        assertThat(pendingCount).isEqualTo(2);
    }

    @Test
    void propose_unknownInvitee_throwsException() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);

        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            meetingService.propose(organizer, "Title", "Desc", start, end, List.of("unknown"));
        });
    }

    @Test
    void respond_validStatus_updatesParticipant() {
        Meeting meeting = new Meeting("t", "d", Instant.now(), Instant.now().plusSeconds(60), organizer);
        MeetingParticipant participant = new MeetingParticipant(meeting, organizer, InviteStatus.PENDING);
        when(participantRepository.findByMeetingIdAndUserId(10L, 1L)).thenReturn(Optional.of(participant));

        meetingService.respond(10L, organizer, InviteStatus.ACCEPTED);

        assertThat(participant.getStatus()).isEqualTo(InviteStatus.ACCEPTED);
    }

    @Test
    void respond_invalidStatus_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            meetingService.respond(10L, organizer, InviteStatus.PENDING);
        });
    }

    @Test
    void respond_participantNotFound_throwsException() {
        when(participantRepository.findByMeetingIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            meetingService.respond(10L, organizer, InviteStatus.ACCEPTED);
        });
    }

    @Test
    void copyFromDiscovered_withNullEnd_defaultsToTwoHours() {
        Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        DiscoveredEvent event = new DiscoveredEvent("Source", "extId", "Event Title", "Desc", start, null, "url",
                "venue");

        when(meetingRepository.save(any(Meeting.class))).thenAnswer(i -> i.getArguments()[0]);

        Meeting meeting = meetingService.copyFromDiscovered(organizer, event);

        assertThat(meeting.getEndTime()).isEqualTo(start.plus(Duration.ofHours(2)));
        assertThat(meeting.getParticipants()).hasSize(1);
        assertThat(meeting.getParticipants().iterator().next().getStatus()).isEqualTo(InviteStatus.ACCEPTED);
    }
}
