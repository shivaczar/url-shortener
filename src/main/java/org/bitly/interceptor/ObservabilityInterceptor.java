package org.bitly.interceptor;

import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ObservabilityInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(ObservabilityInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("startTime", System.currentTimeMillis());
        logger.info("Incoming request: {} {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long startTime = (Long) request.getAttribute("startTime");
        long duration = System.currentTimeMillis() - startTime;

        logger.info("Completed request: {} {} | Status: {} | Time: {}ms",
                request.getMethod(), request.getRequestURI(), response.getStatus(), duration);

        // Send logs to Sentry
        Sentry.addBreadcrumb("Request", request.getRequestURI());
        Sentry.captureMessage("Request completed in " + duration + "ms");

        // Capture exceptions if any
        if (ex != null) {
            Sentry.captureException(ex);
        }
    }
}

