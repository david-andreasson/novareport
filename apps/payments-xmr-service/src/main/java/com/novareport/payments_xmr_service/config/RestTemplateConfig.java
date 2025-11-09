package com.novareport.payments_xmr_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Value("${resttemplate.connect-timeout-seconds:5}")
    private int connectTimeoutSeconds;

    @Value("${resttemplate.read-timeout-seconds:10}")
    private int readTimeoutSeconds;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .readTimeout(Duration.ofSeconds(readTimeoutSeconds))
                .build();
    }
}
