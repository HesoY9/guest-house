package com.hesoy9.guesthouse.controller;

import com.hesoy9.guesthouse.dto.LoginRequest;
import com.hesoy9.guesthouse.dto.UserCreateRequest;
import com.hesoy9.guesthouse.dto.UserResponse;
import com.hesoy9.guesthouse.entity.User;
import com.hesoy9.guesthouse.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserCreateRequest request) {
        User user = userService.createUser(request.getUsername(), request.getPassword(), request.getRole());
        return ResponseEntity.ok(toResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(toResponse(user));
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<Void> disableUser(@PathVariable Long id) {
        userService.disableUser(id);
        return ResponseEntity.noContent().build();
    }

    // maps the entity to a response that leaves passwordHash out entirely
    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.getEnabled());
    }
}
