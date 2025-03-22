package org.bitly.config;

import org.bitly.interceptor.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoggingInterceptor loggingInterceptor;

    @Autowired
    private ApiKeyInterceptor apiKeyInterceptor;

    @Autowired
    private AuthorizationInterceptor  authorizationInterceptor;

    @Autowired
    private BlacklistInterceptor blacklistInterceptor;

    @Autowired
    private ResponseTimeInterceptor responseTimeInterceptor;

    @Autowired
    private AuthenticationInterceptor authenticationInterceptor;

    @Autowired
    private ObservabilityInterceptor observabilityInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor); // Logs every request (should be first)
        registry.addInterceptor(blacklistInterceptor) // Blocks requests from blacklisted API keys
                .addPathPatterns("/api/urls/**");
        registry.addInterceptor(apiKeyInterceptor) // Validates API key before proceeding
                .addPathPatterns("/api/urls/**");
        registry.addInterceptor(authenticationInterceptor) // Authenticates user based on API key
                .addPathPatterns("/api/**");
        registry.addInterceptor(authorizationInterceptor) // Ensures only enterprise users can access bulk shortening
                .addPathPatterns("/api/urls/shorten/batch");
        registry.addInterceptor(responseTimeInterceptor) // Logs response time (should be last)
                .addPathPatterns("/api/**");
        registry.addInterceptor(observabilityInterceptor).addPathPatterns("/api/**");
    }


}

