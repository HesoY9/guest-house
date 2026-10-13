package com.hesoy9.guesthouse.web;

import com.hesoy9.guesthouse.dto.UserCreateRequest;
import com.hesoy9.guesthouse.entity.Role;
import com.hesoy9.guesthouse.entity.User;
import com.hesoy9.guesthouse.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/web/users")
public class UserWebController {

    private static final String FORM_DRAWER = "staff/drawers/user-form :: drawer";

    private final UserService userService;

    public UserWebController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("sectionsByRole", StaffSection.values());
        model.addAttribute("roles", Role.values());
        return "staff/users";
    }

    @GetMapping("/new")
    public String newUserForm(Model model) {
        model.addAttribute("roles", Role.values());
        return FORM_DRAWER;
    }

    @PostMapping
    public Object createUser(@Valid @ModelAttribute UserCreateRequest request,
                             BindingResult bindingResult, Model model, HttpServletResponse response) {
        String error = bindingResult.hasErrors() ? ValidationUtil.firstErrorMessage(bindingResult) : null;
        if (error == null) {
            try {
                User user = userService.createUser(request.getUsername(), request.getPassword(), request.getRole()); // FR33
                return Hx.done("Account " + user.getUsername() + " created (" + user.getRole().getLabel() + ").");
            } catch (IllegalArgumentException | IllegalStateException ex) {
                error = ex.getMessage();
            }
        }
        model.addAttribute("roles", Role.values());
        model.addAttribute("form", request);
        model.addAttribute("errorMessage", error);
        Hx.toast(response, "error", error);
        return FORM_DRAWER;
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<Void> disableUser(@PathVariable Long id, HttpSession session) {
        userService.disableUser(id, (String) session.getAttribute(AuthInterceptor.SESSION_USER)); // FR34
        return Hx.done("Account disabled - they can no longer log in.");
    }

    @PutMapping("/{id}/enable")
    public ResponseEntity<Void> enableUser(@PathVariable Long id) {
        userService.enableUser(id);
        return Hx.done("Account re-enabled.");
    }
}
