package com.hesoy9.guesthouse.web;

import com.hesoy9.guesthouse.dto.LoginRequest;
import com.hesoy9.guesthouse.entity.User;
import com.hesoy9.guesthouse.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/web")
public class AuthWebController {

    private final UserService userService;

    public AuthWebController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // Plain form post, not HTMX - a full page navigation makes sense for login.
    @PostMapping("/login")
    public String login(@ModelAttribute LoginRequest request, HttpSession session, Model model) {
        try {
            User user = userService.login(request.getUsername(), request.getPassword()); // FR30
            session.setAttribute("loggedInUser", user.getUsername());
            session.setAttribute("role", user.getRole()); // used later for FR32 role checks
            return "redirect:/web/rooms";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // FR31
        return "redirect:/web/login";
    }
}
