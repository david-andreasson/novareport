package com.novareport.reporter_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class CorsConfigTest {

    @Test
    void corsConfigurationSourceUsesWildcardWhenNoOriginsConfigured() {
        CorsConfig config = new CorsConfig();

        CorsConfigurationSource source = config.corsConfigurationSource("");
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOriginPatterns()).contains("*");
        assertThat(cors.getAllowedMethods()).contains("GET", "POST", "PUT", "OPTIONS");
        assertThat(cors.getAllowedHeaders()).contains("Content-Type", "Authorization");
    }

    @Test
    void corsConfigurationSourceParsesExplicitOrigins() {
        CorsConfig config = new CorsConfig();

        CorsConfigurationSource source = config.corsConfigurationSource("https://a.example.com, https://b.example.com");
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).containsExactlyInAnyOrder("https://a.example.com", "https://b.example.com");
        assertThat(cors.getAllowedOriginPatterns()).isNullOrEmpty();
    }

    @Test
    void corsFilterBeanIsCreated() {
        CorsConfig config = new CorsConfig();

        CorsConfigurationSource source = config.corsConfigurationSource("*");

        assertThat(config.corsFilter(source)).isNotNull();
    }
}
