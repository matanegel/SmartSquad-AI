package com.smartsquad.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * AppConfig
 * Centralized configuration class for Spring Beans.
 */
@Configuration
public class AppConfig {

    /**
     * Exposes RestTemplate as a Spring Bean.
     * Spring will automatically find this method because the class is annotated with @Configuration.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}