package com.example.meetings.service;

import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void register_usernameTaken_throwsException() {
        when(userRepository.existsByUsername("tiago")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            userService.register("tiago", "tiago@example.com", "pass");
        });
    }

    @Test
    void register_validData_encodesPasswordAndSaves() {
        when(userRepository.existsByUsername("tiago")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User user = userService.register("tiago", "tiago@example.com", "pass");

        assertThat(user.getUsername()).isEqualTo("tiago");
        assertThat(user.getPasswordHash()).isEqualTo("encoded_pass");
        verify(userRepository).save(user);
    }

    @Test
    void requireByUsername_userExists_returnsUser() {
        User user = new User("tiago", "tiago@example.com", "encoded_pass");
        when(userRepository.findByUsername("tiago")).thenReturn(Optional.of(user));

        User result = userService.requireByUsername("tiago");
        assertThat(result).isEqualTo(user);
    }

    @Test
    void requireByUsername_userNotFound_throwsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.requireByUsername("unknown");
        });
        assertThat(ex.getMessage()).contains("Unknown user: unknown");
    }
}
