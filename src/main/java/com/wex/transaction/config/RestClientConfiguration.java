package com.wex.transaction.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration class for REST client beans
 * 
 * This configuration sets up:
 * - RestTemplate: HTTP client for calling external APIs (Treasury API)
 * - Timeout settings: Prevents hanging requests
 * - Error handling: Consistent error response handling
 * 
 * @author Bo
 */
@Configuration
@Profile("!test")
public class RestClientConfiguration {

    /**
     * Create and configure a RestTemplate bean
     * 
     * RestTemplate provides:
     * - Simple HTTP client functionality
     * - Integration with Spring's HttpMessageConverter for JSON serialization
     * - Clean API for making HTTP requests
     * 
     * Configuration includes:
     * - Request timeout: 15 seconds (configurable via properties)
     * - Read timeout: 15 seconds
     * 
     * @param builder Spring's RestTemplateBuilder for fluent configuration
     * @return Configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(15))
                .setReadTimeout(Duration.ofSeconds(15))
                .build();
    }
}
