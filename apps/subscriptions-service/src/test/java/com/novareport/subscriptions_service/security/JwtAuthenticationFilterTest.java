package com.novareport.subscriptions_service.security;

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
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

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
    void shouldNotFilterReturnsTrueForExcludedPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/login");

        boolean result = filter.shouldNotFilter(request);

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotFilterReturnsFalseForProtectedPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/subscriptions/me");

        boolean result = filter.shouldNotFilter(request);

        assertThat(result).isFalse();
    }

    @Test
    void doFilterInternalSetsAuthenticationWhenTokenValid() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/subscriptions/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Claims claims = mock(Claims.class);
        when(jwtService.parse("valid-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("user1");
        when(claims.get("role")).thenReturn("ADMIN");
        when(claims.get("uid")).thenReturn("uid-123");

        AtomicBoolean called = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> {
            called.set(true);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(req.getAttribute("uid")).isEqualTo("uid-123");
        };

        filter.doFilterInternal(request, response, chain);

        assertThat(called).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void doFilterInternalClearsContextOnJwtException() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/subscriptions/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.parse("bad-token")).thenThrow(new RuntimeException("invalid"));

        AtomicBoolean called = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> called.set(true);

        filter.doFilterInternal(request, response, chain);

        assertThat(called).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternalDoesNothingWhenNoAuthorizationHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/subscriptions/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean called = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> called.set(true);

        filter.doFilterInternal(request, response, chain);

        assertThat(called).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }
}
