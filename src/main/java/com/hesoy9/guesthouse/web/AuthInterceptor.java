package com.hesoy9.guesthouse.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false); // don't create one just to check
        if (session != null && session.getAttribute("loggedInUser") != null) {
            return true; // logged in - let the request through
        }
        response.sendRedirect(request.getContextPath() + "/web/login");
        return false; // not logged in - stop here, don't call the controller at all
    }
}
