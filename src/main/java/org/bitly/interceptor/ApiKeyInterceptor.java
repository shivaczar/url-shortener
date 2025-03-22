package org.bitly.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bitly.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyInterceptor.class);

    @Autowired
    private UserRepository userRepository; // Assume UserRepository stores valid API keys

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        request.setAttribute("startTime-ApiKeyInterceptor", System.currentTimeMillis());
        String apiKey = request.getHeader("X-API-KEY");

        // Validate API key
        if (apiKey == null || !userRepository.existsByApiKey(apiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid API Key");
            return false; // Stop request processing
        }

        return true; // Allow request to continue
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long startTime = (long) request.getAttribute("startTime-ApiKeyInterceptor");
        long duration = System.currentTimeMillis() - startTime;
        logger.info("ApiKeyInterceptor took {} ms", duration);
    }
}

