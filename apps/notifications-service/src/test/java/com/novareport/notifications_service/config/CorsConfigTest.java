package com.novareport.notifications_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    private final CorsConfig corsConfig = new CorsConfig();

    @Test
    void corsConfigurationSourceWithDefaultOrigins() {
        CorsConfigurationSource source = corsConfig.corsConfigurationSource("*");

        assertThat(source).isNotNull();
    }

    @Test
    void corsConfigurationSourceWithMultipleOrigins() {
        CorsConfigurationSource source = corsConfig.corsConfigurationSource("http://localhost:5173,https://example.com");

        assertThat(source).isNotNull();
    }

    @Test
    void corsConfigurationSourceWithWildcardPattern() {
        CorsConfigurationSource source = corsConfig.corsConfigurationSource("https://*.example.com");

        assertThat(source).isNotNull();
    }

    @Test
    void corsFilterCreated() {
        CorsConfigurationSource source = corsConfig.corsConfigurationSource("*");
        CorsFilter filter = corsConfig.corsFilter(source);

        assertThat(filter).isNotNull();
    }

    @Test
    void corsConfigurationSourceWithEmptyOrigins() {
        CorsConfigurationSource source = corsConfig.corsConfigurationSource("");

        assertThat(source).isNotNull();
    }
}
