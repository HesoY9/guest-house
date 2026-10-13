package com.hesoy9.guesthouse.site;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;

// Shared by every public-site controller: who's signed in (for the nav bar), today's date
// (for date pickers), and a friendly page instead of a stack trace when something isn't found.
@ControllerAdvice(basePackages = "com.hesoy9.guesthouse.site")
public class SiteModelAdvice {

    private static final Logger log = LoggerFactory.getLogger(SiteModelAdvice.class);

    // principal is null when nobody is signed in - Spring just injects null, no exception.
    @ModelAttribute
    public void addSiteContext(@AuthenticationPrincipal OAuth2User principal, Model model) {
        model.addAttribute("googleUserName", principal != null ? principal.getAttribute("name") : null);
        model.addAttribute("today", LocalDate.now());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String notFound(IllegalArgumentException ex, HttpServletResponse response, Model model) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        model.addAttribute("title", "We couldn't find that");
        model.addAttribute("message", "The room or booking you're looking for doesn't exist.");
        return "site/message";
    }

    @ExceptionHandler(IllegalStateException.class)
    public String ruleViolation(IllegalStateException ex, HttpServletResponse response, Model model) {
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        model.addAttribute("title", "That didn't work");
        model.addAttribute("message", ex.getMessage());
        return "site/message";
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public String badParameter(HttpServletRequest request, HttpServletResponse response, Model model) {
        log.debug("Bad parameter on {}", request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        model.addAttribute("title", "That link doesn't look right");
        model.addAttribute("message", "Please go back and try your search again.");
        return "site/message";
    }
}
