package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.Role;
import com.hesoy9.guesthouse.entity.User;
import com.hesoy9.guesthouse.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Hash of a throwaway password - checked against when the username doesn't exist, so a
    // wrong username takes as long as a wrong password and can't be told apart by timing.
    private final String dummyHash = passwordEncoder.encode("not-a-real-password");

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String username, String rawPassword, Role role) { // FR33
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (userRepository.findByUsername(username.trim()).isPresent()) {
            throw new IllegalStateException("Username " + username.trim() + " is already taken");
        }
        User user = new User();
        user.setUsername(username.trim());
        user.setPasswordHash(passwordEncoder.encode(rawPassword)); // NFR3
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public User login(String username, String rawPassword) { // FR30
        Optional<User> found = userRepository.findByUsername(username);
        if (found.isEmpty()) {
            passwordEncoder.matches(rawPassword == null ? "" : rawPassword, dummyHash);
            throw new IllegalArgumentException("Invalid username or password");
        }
        User user = found.get();

        // Password first: "account disabled" is only revealed to someone who knows the password.
        if (rawPassword == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        if (!user.getEnabled()) {
            throw new IllegalStateException("This account has been disabled");
        }
        return user; // FR32: read user.getRole() wherever you need to restrict an action
    }

    public void disableUser(Long userId) { // FR34
        disableUser(userId, null);
    }

    // actingUsername: the admin doing it - they can't lock themselves out.
    public void disableUser(Long userId, String actingUsername) {
        User user = getUser(userId);
        if (user.getUsername().equals(actingUsername)) {
            throw new IllegalStateException("You can't disable your own account");
        }
        if (user.getRole() == Role.ADMIN && user.getEnabled()
                && userRepository.findByRole(Role.ADMIN).stream().filter(User::getEnabled).count() <= 1) {
            throw new IllegalStateException("At least one administrator account must stay active");
        }
        user.setEnabled(false);
        userRepository.save(user);
    }

    public void enableUser(Long userId) {
        User user = getUser(userId);
        user.setEnabled(true);
        userRepository.save(user);
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
