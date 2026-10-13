package com.hesoy9.guesthouse.config;

import com.hesoy9.guesthouse.service.PhotoStorageService;
import com.hesoy9.guesthouse.web.AuthInterceptor;
import com.hesoy9.guesthouse.web.RoleAccessInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final PhotoStorageService photoStorageService;

    public WebConfig(AuthInterceptor authInterceptor, PhotoStorageService photoStorageService) {
        this.authInterceptor = authInterceptor;
        this.photoStorageService = photoStorageService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/web", "/web/**")
                // must exclude the login page itself, or logging in becomes impossible (redirect loop)
                .excludePathPatterns("/web/login", "/web/logout");
        // Note: this only guards /web/** - the REST API under /api/** is controlled by
        // site.rest-api-enabled in SecurityConfig.

        // Registered second, so it runs after AuthInterceptor above for any matching request -
        // by the time this fires, we already know the person is logged in.
        registry.addInterceptor(new RoleAccessInterceptor())
                .addPathPatterns("/web", "/web/**")
                .excludePathPatterns("/web/login", "/web/logout");
    }

    // Uploaded room photos live on disk outside the app (see PhotoStorageService).
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            Files.createDirectories(photoStorageService.getRoot());
        } catch (IOException ex) {
            throw new UncheckedIOException("Can't create the upload folder " + photoStorageService.getRoot(), ex);
        }
        String location = photoStorageService.getRoot().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler(PhotoStorageService.URL_PREFIX + "**").addResourceLocations(location);
    }
}
