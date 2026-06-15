package com.example.meetings.repository;

import com.ninja_squad.dbsetup.Operations;
import com.ninja_squad.dbsetup.operation.Operation;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.insertInto;

public class DbSetupUtils {

        private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        public static final Operation DELETE_ALL = deleteAllFrom("meeting_participants", "meetings", "users");

        public static final Operation INSERT_USERS = insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1L, "tiago", "tiago@example.com", passwordEncoder.encode("pass"), "token-tiago")
                        .values(2L, "moura", "moura@example.com", passwordEncoder.encode("pass"), "token-moura")
                        .values(3L, "pkto", "pkto@example.com", passwordEncoder.encode("pass"), "token-pkto")
                        .build();

        public static final Operation INSERT_MEETINGS = insertInto("meetings")
                        .columns("id", "title", "description", "start_time", "end_time", "organizer_id")
                        .values(1L, "Team Sync", "Weekly sync", Instant.parse("2030-01-01T10:00:00Z"),
                                        Instant.parse("2030-01-01T11:00:00Z"), 1L)
                        .values(2L, "Project Kickoff", "New project", Instant.parse("2030-01-02T14:00:00Z"),
                                        Instant.parse("2030-01-02T15:00:00Z"), 2L)
                        .build();

        public static final Operation INSERT_PARTICIPANTS = insertInto("meeting_participants")
                        .columns("id", "meeting_id", "user_id", "status")
                        .values(1L, 1L, 1L, "ACCEPTED")
                        .values(2L, 1L, 2L, "PENDING")
                        .values(3L, 2L, 2L, "ACCEPTED")
                        .values(4L, 2L, 3L, "DECLINED")
                        .build();

        public static final Operation SEED_DATABASE = Operations.sequenceOf(
                        DELETE_ALL,
                        INSERT_USERS,
                        INSERT_MEETINGS,
                        INSERT_PARTICIPANTS);
}
