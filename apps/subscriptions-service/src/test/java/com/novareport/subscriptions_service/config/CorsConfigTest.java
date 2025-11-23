package com.novareport.subscriptions_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    private final CorsConfig config = new CorsConfig();

    @Test
    void corsConfigurationSourceUsesWildcardWhenNoOriginsConfigured() {
        CorsConfigurationSource source = config.corsConfigurationSource(" ");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/any");
        CorsConfiguration cfg = source.getCorsConfiguration(request);

        assertThat(cfg).isNotNull();
        if (cfg == null) {
            throw new IllegalStateException("CorsConfiguration must not be null");
        }

        assertThat(cfg.getAllowedOriginPatterns()).contains("*");
        assertThat(cfg.getAllowedMethods()).contains("GET", "POST", "PUT", "OPTIONS");
        assertThat(cfg.getAllowedHeaders()).contains("Content-Type", "Authorization");
        assertThat(cfg.getAllowCredentials()).isFalse();
    }

    @Test
    void corsConfigurationSourceParsesExplicitOrigins() {
        CorsConfigurationSource source = config.corsConfigurationSource("https://a.example.com, https://b.example.com");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/any");
        CorsConfiguration cfg = source.getCorsConfiguration(request);

        assertThat(cfg).isNotNull();
        if (cfg == null) {
            throw new IllegalStateException("CorsConfiguration must not be null");
        }

        assertThat(cfg.getAllowedOrigins()).containsExactlyInAnyOrder(
                "https://a.example.com",
                "https://b.example.com"
        );
        assertThat(cfg.getAllowedOriginPatterns()).isNullOrEmpty();
    }

    @Test
    void corsFilterBeanWrapsConfigurationSource() {
        CorsConfigurationSource source = config.corsConfigurationSource("*");

        CorsFilter filter = config.corsFilter(Objects.requireNonNull(source));

        assertThat(filter).isNotNull();
    }
}
