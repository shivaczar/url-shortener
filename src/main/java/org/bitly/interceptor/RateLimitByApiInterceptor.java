package org.bitly.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bitly.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Map;

@Component
public class RateLimitByApiInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisCacheService redisCacheService;

    private final Map<String, Integer> rateLimits = Map.of(
            "/shorten", 10,
            "/redirect", 50
    );

    private final int windowInSeconds = 60; // common window for all endpoints

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String apiKey = request.getHeader("X-API-Key");

        if (apiKey == null || apiKey.isBlank()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Missing or invalid API key.");
            return false;
        }

        String path = request.getRequestURI();
        int limit = rateLimits.getOrDefault(path, 100);

        String redisKey = "rate:" + apiKey + ":" + path;

        long currentCount = redisCacheService.increment(redisKey, windowInSeconds);
        long remaining = Math.max(0, limit - currentCount);
        long resetTimestamp = Instant.now().getEpochSecond() + redisCacheService.ttl(redisKey);

        // Set rate-limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetTimestamp));

        if (currentCount > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Rate limit exceeded for API key.");
            return false;
        }

        return true;
    }
}
