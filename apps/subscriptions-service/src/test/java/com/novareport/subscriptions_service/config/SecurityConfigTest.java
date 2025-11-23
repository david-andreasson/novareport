package com.novareport.subscriptions_service.config;

import com.novareport.subscriptions_service.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Answers.RETURNS_SELF;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    void securityFilterChainBuildsFilterChain() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_SELF);
        DefaultSecurityFilterChain builtChain = mock(DefaultSecurityFilterChain.class);
        when(http.build()).thenReturn(builtChain);

        SecurityConfig config = new SecurityConfig();

        SecurityFilterChain result = config.securityFilterChain(
                http,
                jwtAuthenticationFilter,
                "internal-key",
                corsConfigurationSource
        );

        assertThat(result).isSameAs(builtChain);
        verify(http, times(2)).addFilterBefore(any(), org.mockito.ArgumentMatchers.eq(UsernamePasswordAuthenticationFilter.class));
        verify(http).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
