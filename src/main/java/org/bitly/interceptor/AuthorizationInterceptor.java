package org.bitly.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bitly.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        request.setAttribute("startTime-AuthorizationInterceptor", System.currentTimeMillis());
        // Get user object from the request (set by AuthenticationInterceptor)
        User user = (User) request.getAttribute("authenticatedUser");

        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated");
            return false;
        }

        // Check if user is allowed to perform bulk shortening
        if (!"enterprise".equalsIgnoreCase(user.getTier())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bulk URL creation is only available for enterprise users");
            return false;
        }

        return true;  // Continue to the controller
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long startTime = (long) request.getAttribute("startTime-AuthorizationInterceptor");
        long duration = System.currentTimeMillis() - startTime;
        logger.info("AuthorizationInterceptor took {} ms", duration);
    }
}

