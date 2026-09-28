package com.hesoy9.guesthouse.config;

import com.hesoy9.guesthouse.entity.Role;
import com.hesoy9.guesthouse.repository.UserRepository;
import com.hesoy9.guesthouse.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    public DataSeeder(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userService.createUser("admin", "admin123", Role.ADMIN);
            System.out.println("No users found - seeded default admin (username: admin / password: admin123)");
        }
    }
}
