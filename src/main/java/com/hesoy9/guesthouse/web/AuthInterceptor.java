package com.hesoy9.guesthouse.web;

import com.hesoy9.guesthouse.entity.User;
import com.hesoy9.guesthouse.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

// Step 1 of staff security: is anyone logged in? Runs before RoleAccessInterceptor.
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_USER = "loggedInUser";
    public static final String SESSION_ROLE = "role";

    private final UserService userService;

    public AuthInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false); // don't create one just to check
        Object username = session != null ? session.getAttribute(SESSION_USER) : null;

        if (username != null) {
            // Re-read the account on every request, so disabling a user or changing their role
            // takes effect immediately instead of when their session happens to expire.
            Optional<User> user = userService.findByUsername(username.toString());
            if (user.isPresent() && user.get().getEnabled()) {
                session.setAttribute(SESSION_ROLE, user.get().getRole());
                return true; // logged in - let the request through
            }
            session.invalidate();
        }

        // The browser sent a session cookie the server no longer knows -> the session timed out.
        boolean expired = username != null
                || (request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid());
        String loginUrl = request.getContextPath() + "/web/login" + (expired ? "?expired" : "");
        if (Hx.isHtmx(request)) {
            // A plain redirect would load the login page *inside* the panel being updated.
            response.setHeader("HX-Redirect", loginUrl);
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.sendRedirect(loginUrl);
        }
        return false; // not logged in - stop here, don't call the controller at all
    }
}
