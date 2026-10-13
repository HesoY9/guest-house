package com.hesoy9.guesthouse.web;

import com.hesoy9.guesthouse.entity.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Shared by every staff (/web/**) controller:
//  - puts the logged-in user, their role and their menu into every page's model
//  - turns service exceptions into a toast (HTMX) or a friendly error page (full page loads)
@ControllerAdvice(basePackages = "com.hesoy9.guesthouse.web")
public class StaffModelAdvice {

    private static final Logger log = LoggerFactory.getLogger(StaffModelAdvice.class);

    @ModelAttribute
    public void addStaffContext(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        Object user = session != null ? session.getAttribute(AuthInterceptor.SESSION_USER) : null;
        Role role = session != null ? (Role) session.getAttribute(AuthInterceptor.SESSION_ROLE) : null;
        List<StaffSection> sections = StaffSection.visibleTo(role);

        model.addAttribute("currentUser", user);
        model.addAttribute("currentRole", role);
        model.addAttribute("navSections", sections);
        // allowed.contains('PAYMENTS') in a template = may this user open the Payments pages?
        Set<String> allowed = sections.stream().map(Enum::name).collect(Collectors.toSet());
        model.addAttribute("allowed", allowed);
        model.addAttribute("today", LocalDate.now());
        String query = request.getQueryString();
        model.addAttribute("currentUrl", request.getRequestURI() + (query != null ? "?" + query : ""));
    }

    // Business-rule and "not found" errors from the services carry messages written for people.
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public Object handleRuleViolation(RuntimeException ex, HttpServletRequest request,
                                      HttpServletResponse response, Model model) {
        if (Hx.isHtmx(request)) {
            return Hx.fail(ex.getMessage());
        }
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        model.addAttribute("message", ex.getMessage());
        return "staff/error";
    }

    @ExceptionHandler(Exception.class)
    public Object handleUnexpected(Exception ex, HttpServletRequest request,
                                   HttpServletResponse response, Model model) throws Exception {
        // Let Spring handle the ones it already knows how to answer (404, 405, ...).
        if (ex instanceof org.springframework.web.ErrorResponse) {
            throw ex;
        }
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        if (Hx.isHtmx(request)) {
            return Hx.fail("Something went wrong - please try again.");
        }
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        model.addAttribute("message", "Something went wrong - please try again.");
        return "staff/error";
    }
}
