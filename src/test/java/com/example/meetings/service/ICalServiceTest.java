package com.example.meetings.service;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ICalServiceTest {

    private final ICalService iCalService = new ICalService();

    @Test
    void render_emptyMeetings_returnsBasicCalendar() {
        User owner = new User("alice", "alice@example.com", "pass");
        String result = iCalService.render(owner, List.of());

        assertThat(result).contains("BEGIN:VCALENDAR");
        assertThat(result).contains("X-WR-CALNAME:alice's meetings");
        assertThat(result).contains("END:VCALENDAR");
        assertThat(result).doesNotContain("BEGIN:VEVENT");
    }

    @Test
    void render_withMeetings_rendersEventsCorrectly() {
        User organizer = new User("alice", "alice@test.com", "pass");
        User invitee = new User("bob", "bob@test.com", "pass");

        Instant start = Instant.parse("2026-07-01T10:00:00Z");
        Instant end = Instant.parse("2026-07-01T11:00:00Z");
        
        Meeting meeting = new Meeting("Test Meeting", "Desc with \n newline", start, end, organizer);
        org.springframework.test.util.ReflectionTestUtils.setField(meeting, "id", 10L);
        meeting.addParticipant(new MeetingParticipant(meeting, organizer, InviteStatus.ACCEPTED));
        meeting.addParticipant(new MeetingParticipant(meeting, invitee, InviteStatus.PENDING));

        String result = iCalService.render(organizer, List.of(meeting));

        assertThat(result).contains("BEGIN:VEVENT");
        assertThat(result).contains("UID:meeting-10@meetings-app");
        assertThat(result).contains("SUMMARY:Test Meeting");
        assertThat(result).contains("DESCRIPTION:Desc with \\n newline");
        assertThat(result).contains("ORGANIZER;CN=alice:mailto:alice@test.com");
        assertThat(result).contains("ATTENDEE;CN=bob;PARTSTAT=NEEDS-ACTION:mailto:bob@test.com");
        assertThat(result).contains("STATUS:TENTATIVE");
        assertThat(result).contains("END:VEVENT");
    }
}
