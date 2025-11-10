package com.novareport.accounts_service.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${cors.allowed-origins:*}") String rawOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        applyAllowedOrigins(configuration, rawOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public CorsFilter corsFilter(CorsConfigurationSource corsConfigurationSource) {
        return new CorsFilter(corsConfigurationSource);
    }

    private void applyAllowedOrigins(CorsConfiguration configuration, String rawOrigins) {
        List<String> origins = Arrays.stream(rawOrigins.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .collect(Collectors.toList());

        if (origins.isEmpty()) {
            configuration.addAllowedOriginPattern("*");
            return;
        }

        for (String origin : origins) {
            if ("*".equals(origin) || origin.contains("*")) {
                configuration.addAllowedOriginPattern(origin);
            } else {
                configuration.addAllowedOrigin(origin);
            }
        }
    }
}
