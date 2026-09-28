package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.User;
import com.hesoy9.guesthouse.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // real BCrypt encoder just to build realistic test data - not mocked,
    // since UserService creates its own encoder internally rather than having it injected
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void login_succeedsWithCorrectPassword() {
        User user = new User();
        user.setUsername("admin");
        user.setPasswordHash(encoder.encode("secret123"));
        user.setEnabled(true);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        User result = userService.login("admin", "secret123");

        assertEquals("admin", result.getUsername());
    }

    @Test
    void login_failsWithWrongPassword() {
        User user = new User();
        user.setUsername("admin");
        user.setPasswordHash(encoder.encode("secret123"));
        user.setEnabled(true);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> userService.login("admin", "wrongpass"));
    }

    @Test
    void login_failsWhenAccountDisabled() {
        User user = new User();
        user.setUsername("admin");
        user.setPasswordHash(encoder.encode("secret123"));
        user.setEnabled(false);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class, () -> userService.login("admin", "secret123"));
    }

    @Test
    void login_failsWhenUsernameNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userService.login("ghost", "whatever"));
    }
}
