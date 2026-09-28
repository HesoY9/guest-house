package com.hesoy9.guesthouse.web;

import com.hesoy9.guesthouse.dto.UserCreateRequest;
import com.hesoy9.guesthouse.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/web/users")
public class UserWebController {

    private final UserService userService;

    public UserWebController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "users";
    }

    @PostMapping
    public String createUser(@Valid @ModelAttribute UserCreateRequest request,
                              BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", ValidationUtil.firstErrorMessage(bindingResult));
            model.addAttribute("users", userService.getAllUsers());
            return "fragments/users-panel :: panel";
        }
        try {
            userService.createUser(request.getUsername(), request.getPassword(), request.getRole()); // FR33
            model.addAttribute("successMessage", "User created.");
        } catch (RuntimeException ex) {
            // e.g. duplicate username hitting the unique constraint
            model.addAttribute("errorMessage", "Could not create user - username may already be taken.");
        }
        model.addAttribute("users", userService.getAllUsers());
        return "fragments/users-panel :: panel";
    }

    @PutMapping("/{id}/disable")
    public String disableUser(@PathVariable Long id, Model model) {
        userService.disableUser(id); // FR34
        model.addAttribute("successMessage", "User disabled.");
        model.addAttribute("users", userService.getAllUsers());
        return "fragments/users-panel :: panel";
    }
}
