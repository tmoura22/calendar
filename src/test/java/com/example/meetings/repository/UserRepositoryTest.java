package com.example.meetings.repository;

import com.example.meetings.model.User;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import javax.sql.DataSource;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    private static final DbSetupTracker dbSetupTracker = new DbSetupTracker();

    @BeforeEach
    void setUp() {
        DbSetup dbSetup = new DbSetup(new DataSourceDestination(dataSource), DbSetupUtils.SEED_DATABASE);
        dbSetupTracker.launchIfNecessary(dbSetup);
    }

    @Test
    void findByUsername_existingUser_returnsUser() {
        dbSetupTracker.skipNextLaunch();
        Optional<User> user = userRepository.findByUsername("tiago");
        assertThat(user).isPresent();
        assertThat(user.get().getEmail()).isEqualTo("tiago@example.com");
    }

    @Test
    void findByUsername_missingUser_returnsEmpty() {
        dbSetupTracker.skipNextLaunch();
        Optional<User> user = userRepository.findByUsername("unknown");
        assertThat(user).isEmpty();
    }

    @Test
    void findByIcalToken_existingToken_returnsUser() {
        dbSetupTracker.skipNextLaunch();
        Optional<User> user = userRepository.findByIcalToken("token-moura");
        assertThat(user).isPresent();
        assertThat(user.get().getUsername()).isEqualTo("moura");
    }

    @Test
    void existsByUsername_returnsTrueForExisting() {
        dbSetupTracker.skipNextLaunch();
        assertThat(userRepository.existsByUsername("pkto")).isTrue();
    }
}
