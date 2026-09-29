package com.hesoy9.guesthouse.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Keep this false whenever the site is reachable from the internet: the REST API has no
    // login of its own. Set site.rest-api-enabled=true only on your own machine for Postman testing.
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

                // Public pages (home, search, css, images) and the staff app under /web/**.
                // Staff pages are protected by your own AuthInterceptor, not by Google login -
                // being signed in with Google must never grant access to them.
                auth.anyRequest().permitAll();
            })
            .oauth2Login(Customizer.withDefaults())
            .logout(logout -> logout.logoutSuccessUrl("/"))
            // The staff pages use HTMX requests without CSRF tokens, so they stay exempt (as before).
            // Guest forms (/book, /my-bookings, /logout) are CSRF-protected automatically.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/web/**", "/api/**"));

        return http.build();
    }
}
