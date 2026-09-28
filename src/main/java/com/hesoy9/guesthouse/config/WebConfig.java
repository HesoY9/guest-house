package com.hesoy9.guesthouse.config;

import com.hesoy9.guesthouse.web.AdminOnlyInterceptor;
import com.hesoy9.guesthouse.web.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/web/**")
                // must exclude the login page itself, or logging in becomes impossible (redirect loop)
                .excludePathPatterns("/web/login", "/web/logout", "/css/**", "/js/**");
        // Note: this only guards /web/** - your REST API under /api/** stays open,
        // so Postman testing keeps working exactly as before.

        // Registered second, so it runs after AuthInterceptor above for any matching request -
        // by the time this fires, we already know the person is logged in.
        registry.addInterceptor(new AdminOnlyInterceptor())
                .addPathPatterns("/web/users/**");
    }
}
