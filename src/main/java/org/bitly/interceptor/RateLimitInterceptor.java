package org.bitly.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bitly.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisCacheService redisCacheService;

    private final Map<String, Integer> rateLimits = Map.of(
            "/shorten", 10,
            "/redirect", 50
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();

        // Match only exact endpoints
        int limit = rateLimits.getOrDefault(uri, 100); // default fallback if needed

        boolean allowed = redisCacheService.isAllowed(ip, uri, limit);
        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Rate limit exceeded. Try again later.");
            return false;
        }

        return true;
    }
}

