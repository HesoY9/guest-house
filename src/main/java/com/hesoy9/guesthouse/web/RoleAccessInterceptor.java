package com.hesoy9.guesthouse.web;

import com.hesoy9.guesthouse.entity.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;
import java.util.Set;

// Step 2 of staff security (runs after AuthInterceptor, so we know someone is logged in):
// may this role open this page? The rules come from StaffSection. Anything under /web/**
// that isn't part of a section is denied, unless it's in the small "any staff" list below -
// so a new page is locked down by default until it's added to a section.
public class RoleAccessInterceptor implements HandlerInterceptor {

    private static final Set<String> ANY_STAFF = Set.of("/web", "/web/");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (ANY_STAFF.contains(path)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        Role role = session != null ? (Role) session.getAttribute(AuthInterceptor.SESSION_ROLE) : null;
        Optional<StaffSection> section = StaffSection.forPath(path);

        if (section.isPresent() && section.get().allows(role)) {
            return true;
        }

        if (Hx.isHtmx(request)) {
            Hx.toast(response, "error", "Your role doesn't have access to that.");
            response.setHeader("HX-Reswap", "none");
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Your role doesn't have access to that page.");
        }
        return false;
    }
}
