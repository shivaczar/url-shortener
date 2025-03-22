package org.bitly.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class BlacklistInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(BlacklistInterceptor.class);

    private static final Set<String> blacklistedKeys = new HashSet<>();

    public BlacklistInterceptor() {
        loadBlacklistedKeys();
    }

    private void loadBlacklistedKeys() {
        try {
            Path path = new ClassPathResource("blacklist.properties").getFile().toPath();
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                if (line.startsWith("blacklisted.keys=")) {
                    String keys = line.split("=")[1].trim();
                    blacklistedKeys.addAll(List.of(keys.split(",")));
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load blacklist file: " + e.getMessage());
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        request.setAttribute("startTime-BlacklistInterceptor", System.currentTimeMillis());
        String apiKey = request.getHeader("X-API-KEY");

        if (apiKey != null && blacklistedKeys.contains(apiKey.trim())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Your API key has been blocked due to suspicious activity.");
            return false;
        }

        return true;  // Proceed with request
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long startTime = (long) request.getAttribute("startTime-BlacklistInterceptor");
        long duration = System.currentTimeMillis() - startTime;
        logger.info("BlacklistInterceptor took {} ms", duration);
    }
}

