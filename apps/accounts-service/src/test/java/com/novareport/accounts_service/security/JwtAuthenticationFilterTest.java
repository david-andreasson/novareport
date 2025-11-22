package com.novareport.accounts_service.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtService);
    }

    @Test
    void shouldNotFilterReturnsTrueForExcludedPaths() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/auth/login"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/h2-console"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/actuator/health"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/swagger-ui/index.html"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/v3/api-docs"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/swagger-resources/configuration/ui"))).isTrue();
    }

    @Test
    void shouldNotFilterReturnsFalseForProtectedPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/accounts/me");

        boolean result = filter.shouldNotFilter(request);

        assertThat(result).isFalse();
    }

    @Test
    void doFilterInternalSetsAuthenticationWhenTokenValid() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/accounts/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Claims claims = mock(Claims.class);
        when(jwtService.parse("valid-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("user@example.com");
        when(claims.get("role")).thenReturn("USER");

        FilterChain chain = (req, res) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getName()).isEqualTo("user@example.com");
            assertThat(auth.getAuthorities()).singleElement()
                    .extracting("authority")
                    .isEqualTo("ROLE_USER");
        };

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void doFilterInternalClearsContextOnJwtException() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/accounts/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.parse("bad-token")).thenThrow(new RuntimeException("invalid"));

        FilterChain chain = (req, res) -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        };

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternalDoesNothingWhenNoAuthorizationHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/accounts/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        };

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }
}
