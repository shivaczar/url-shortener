package org.bitly.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bitly.entity.RequestLog;
import org.bitly.repository.RequestLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);


    @Autowired
    private RequestLogRepository requestLogRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("startTime-LoggingInterceptor", System.currentTimeMillis());
        String requestUri = request.getRequestURI();

        // Only log requests for specific routes
        List<String> monitoredRoutes = List.of("/api/urls/shorten", "/api/urls/delete");
        if (!monitoredRoutes.contains(requestUri)) {
            return true; // Skip logging for other routes
        }

        String userAgent = request.getHeader("User-Agent");
        String ip = request.getRemoteAddr();
        String method = request.getMethod();
        LocalDateTime timestamp = LocalDateTime.now();

        logger.info("Request - Time: {}, Method: {}, URL: {}, IP: {}, User-Agent: {}",
                timestamp, method, requestUri, ip, userAgent);

        // Save to database
        RequestLog log = new RequestLog(timestamp, method, requestUri, userAgent, ip);
        requestLogRepository.save(log);

        return true; // Continue the request
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long startTime = (long) request.getAttribute("startTime-LoggingInterceptor");
        long duration = System.currentTimeMillis() - startTime;
        logger.info("LoggingInterceptor took {} ms", duration);
    }
}

