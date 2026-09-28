package com.hesoy9.guesthouse.web;

import com.hesoy9.guesthouse.entity.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

// Runs AFTER AuthInterceptor for the same request (registration order in WebConfig),
// so by the time this runs, we already know the person is logged in - just checking role here.
public class AdminOnlyInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        Object role = session != null ? session.getAttribute("role") : null;

        if (Role.ADMIN.equals(role)) {
            return true;
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admins only.");
        return false;
    }
}
