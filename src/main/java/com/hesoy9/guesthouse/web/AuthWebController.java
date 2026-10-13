package com.hesoy9.guesthouse.web;

import com.hesoy9.guesthouse.dto.LoginRequest;
import com.hesoy9.guesthouse.entity.Role;
import com.hesoy9.guesthouse.entity.User;
import com.hesoy9.guesthouse.service.LoginAttemptService;
import com.hesoy9.guesthouse.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;

@Controller
@RequestMapping("/web")
public class AuthWebController {

    private final UserService userService;
    private final LoginAttemptService loginAttemptService;

    public AuthWebController(UserService userService, LoginAttemptService loginAttemptService) {
        this.userService = userService;
        this.loginAttemptService = loginAttemptService;
    }

    // "/web" on its own: send each person to the first page their role can use.
    @GetMapping({"", "/"})
    public String home(HttpSession session) {
        return "redirect:" + StaffSection.landingPathFor((Role) session.getAttribute(AuthInterceptor.SESSION_ROLE));
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String expired, HttpSession session, Model model) {
        if (session.getAttribute(AuthInterceptor.SESSION_USER) != null) {
            return "redirect:/web"; // already logged in
        }
        if (expired != null) {
            model.addAttribute("infoMessage", "Your session ended - please log in again.");
        }
        return "staff/login";
    }

    // Plain form post, not HTMX - a full page navigation makes sense for login.
    @PostMapping("/login")
    public String login(@ModelAttribute LoginRequest request, HttpServletRequest httpRequest,
                        Model model, RedirectAttributes redirect) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String ip = httpRequest.getRemoteAddr();
        model.addAttribute("username", username);

        // Login throttling: too many wrong passwords -> locked out for a while.
        Duration wait = loginAttemptService.lockedFor(username, ip);
        if (!wait.isZero()) {
            long minutes = Math.max(1, (wait.toSeconds() + 59) / 60);
            model.addAttribute("errorMessage", "Too many failed attempts. Try again in "
                    + minutes + (minutes == 1 ? " minute." : " minutes."));
            return "staff/login";
        }

        try {
            User user = userService.login(username, request.getPassword()); // FR30
            loginAttemptService.recordSuccess(username, ip);

            // New session id on login, so a session id planted before login is useless (session fixation).
            httpRequest.getSession(true);
            httpRequest.changeSessionId();
            HttpSession session = httpRequest.getSession();
            session.setAttribute(AuthInterceptor.SESSION_USER, user.getUsername());
            session.setAttribute(AuthInterceptor.SESSION_ROLE, user.getRole()); // FR32 role checks

            redirect.addFlashAttribute("toast", "Welcome back, " + user.getUsername() + ".");
            return "redirect:" + StaffSection.landingPathFor(user.getRole());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            loginAttemptService.recordFailure(username, ip);
            model.addAttribute("errorMessage", ex.getMessage());
            return "staff/login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // FR31
        return "redirect:/web/login";
    }
}
