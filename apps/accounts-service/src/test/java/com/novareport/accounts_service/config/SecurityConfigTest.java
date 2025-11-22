package com.novareport.accounts_service.config;

import com.novareport.accounts_service.security.JwtAuthenticationFilter;
import com.novareport.accounts_service.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    @Test
    void filterChainBuildsUsingHttpSecurity() throws Exception {
        SecurityConfig config = new SecurityConfig();
        JwtService jwtService = mock(JwtService.class);
        JwtAuthenticationFilter jwtFilter = config.jwtAuthenticationFilter(jwtService);

        HttpSecurity http = mock(HttpSecurity.class, RETURNS_SELF);
        DefaultSecurityFilterChain chain = mock(DefaultSecurityFilterChain.class);
        CorsConfigurationSource corsSource = mock(CorsConfigurationSource.class);

        when(http.build()).thenReturn(chain);

        SecurityFilterChain result = config.filterChain(http, jwtFilter, "internal-key", corsSource);

        assertThat(result).isSameAs(chain);
        verify(http).build();
    }
}
