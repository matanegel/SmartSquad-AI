package com.smartsquad.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);

        return new RestTemplate(factory);
    }
}