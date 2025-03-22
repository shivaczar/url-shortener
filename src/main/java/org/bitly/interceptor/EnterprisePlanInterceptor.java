package org.bitly.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bitly.entity.User;
import org.bitly.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.Optional;

@Component
public class EnterprisePlanInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(EnterprisePlanInterceptor.class);

    private final UserRepository userRepository;

    public EnterprisePlanInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        request.setAttribute("loggingStartTime", System.currentTimeMillis());
        String apiKey = request.getHeader("X-API-KEY");

        if (apiKey == null || apiKey.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing API Key");
            return false;
        }

        Optional<User> userOpt = userRepository.findByApiKey(apiKey);

        if (userOpt.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid API Key");
            return false;
        }

        User user = userOpt.get();
        if (!"enterprise".equalsIgnoreCase(user.getTier())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Bulk URL creation is only available for enterprise users.");
            return false;
        }

        return true;  // Proceed with the request
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long startTime = (long) request.getAttribute("loggingStartTime");
        long duration = System.currentTimeMillis() - startTime;
        logger.info("EnterprisePlanInterceptor took {} ms", duration);
    }
}

