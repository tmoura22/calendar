package com.example.meetings.repository;

import com.example.meetings.model.Meeting;
import com.example.meetings.model.User;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MeetingRepositoryTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private UserRepository userRepository;

    private static final DbSetupTracker dbSetupTracker = new DbSetupTracker();

    @BeforeEach
    void setUp() {
        DbSetup dbSetup = new DbSetup(new DataSourceDestination(dataSource), DbSetupUtils.SEED_DATABASE);
        dbSetupTracker.launchIfNecessary(dbSetup);
    }

    @Test
    void findCalendarMeetings_forOrganizer_returnsMeetings() {
        dbSetupTracker.skipNextLaunch();
        User tiago = userRepository.findByUsername("tiago").orElseThrow();

        List<Meeting> meetings = meetingRepository.findCalendarMeetings(tiago);

        assertThat(meetings).hasSize(1);
        assertThat(meetings.get(0).getTitle()).isEqualTo("Team Sync");
    }

    @Test
    void findCalendarMeetings_forInvitedPending_returnsMeetings() {
        dbSetupTracker.skipNextLaunch();
        User moura = userRepository.findByUsername("moura").orElseThrow();

        List<Meeting> meetings = meetingRepository.findCalendarMeetings(moura);

        assertThat(meetings).hasSize(2);
    }

    @Test
    void findCalendarMeetings_forDeclined_doesNotReturnMeeting() {
        dbSetupTracker.skipNextLaunch();
        User pkto = userRepository.findByUsername("pkto").orElseThrow();

        List<Meeting> meetings = meetingRepository.findCalendarMeetings(pkto);

        assertThat(meetings).isEmpty();
    }

    @Test
    void findOverlapping_withOverlap_returnsMeetings() {
        dbSetupTracker.skipNextLaunch();
        User tiago = userRepository.findByUsername("tiago").orElseThrow();

        Instant start = Instant.parse("2030-01-01T10:30:00Z");
        Instant end = Instant.parse("2030-01-01T11:30:00Z");

        List<Meeting> overlaps = meetingRepository.findOverlapping(tiago, start, end);
        assertThat(overlaps).hasSize(1);
    }
}
