package com.novareport.payments_stripe_service.config;

import com.novareport.payments_stripe_service.auth.InternalApiKeyFilter;
import com.novareport.payments_stripe_service.auth.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private InternalApiKeyFilter internalApiKeyFilter;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Test
    void securityFilterChainBuildsUsingHttpSecurity() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_SELF);
        DefaultSecurityFilterChain chain = mock(DefaultSecurityFilterChain.class);
        CorsConfigurationSource corsSource = mock(CorsConfigurationSource.class);

        when(http.build()).thenReturn(chain);

        SecurityFilterChain result = securityConfig.securityFilterChain(http, corsSource);

        assertThat(result).isSameAs(chain);
        verify(http).build();
    }

    @Test
    void corsConfigurationSourceParsesOriginsAndPatterns() {
        SecurityConfig config = new SecurityConfig(jwtAuthenticationFilter, internalApiKeyFilter);

        CorsConfigurationSource source = config.corsConfigurationSource("https://example.com,*.novareport.local");
        CorsConfiguration cfg = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(cfg).isNotNull();
        assertThat(cfg.getAllowedOrigins()).contains("https://example.com");
        assertThat(cfg.getAllowedOriginPatterns()).contains("*.novareport.local");
    }
}
