package com.hesoy9.guesthouse.config;

import com.hesoy9.guesthouse.web.Hx;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Keep this false whenever the site is reachable from the internet: the REST API has no
    // login of its own. Set REST_API_ENABLED=true only on your own machine for Postman testing.
    @Value("${site.rest-api-enabled:false}")
    private boolean restApiEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> {
                // Guest-only pages: any signed-in Google account may use these.
                auth.requestMatchers("/book/**", "/my-bookings/**").authenticated();

                if (restApiEnabled) {
                    auth.requestMatchers("/api/**").permitAll();
                } else {
                    auth.requestMatchers("/api/**").denyAll();
                }

                // Public pages (home, rooms, css, images, uploads) and the staff app under /web/**.
                // Staff pages are protected by AuthInterceptor + RoleAccessInterceptor, not by Google
                // login - being signed in with Google must never grant access to them.
                auth.anyRequest().permitAll();
            })
            .oauth2Login(Customizer.withDefaults())
            .logout(logout -> logout.logoutSuccessUrl("/"))
            // CSRF is ON for everything except the (dev-only) REST API, including the staff pages:
            // normal forms carry a hidden _csrf field (Thymeleaf adds it to th:action forms), and
            // staff.js adds the token as a header to every HTMX request.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .exceptionHandling(ex -> ex.accessDeniedHandler(staffAwareAccessDeniedHandler()))
            .headers(headers -> headers
                    .frameOptions(frame -> frame.deny())
                    .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));

        return http.build();
    }

    // A missing/stale CSRF token on a staff page almost always means the session timed out
    // while the page was open. Send them to the login page instead of a bare 403.
    private AccessDeniedHandler staffAwareAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            if (path.startsWith("/web")) {
                String login = request.getContextPath() + "/web/login?expired";
                if (Hx.isHtmx(request)) {
                    response.setHeader("HX-Redirect", login);
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    response.sendRedirect(login);
                }
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
        };
    }
}
