package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.Role;
import com.hesoy9.guesthouse.entity.User;
import com.hesoy9.guesthouse.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String username, String rawPassword, Role role) { // FR33
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword)); // NFR3
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public User login(String username, String rawPassword) { // FR30
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!user.getEnabled()) {
            throw new IllegalStateException("This account has been disabled");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        return user; // FR32: read user.getRole() wherever you need to restrict an action
    }

    public void disableUser(Long userId) { // FR34
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setEnabled(false);
        userRepository.save(user);
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}