package com.novareport.notifications_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class InternalApiKeyFilterTest {

    @Test
    void doesNotFilterNonInternalPaths() throws ServletException, IOException {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean called = new AtomicBoolean(false);

        FilterChain chain = (req, res) -> called.set(true);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isNotEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(called).isTrue();
    }

    @Test
    void returnsForbiddenWhenKeyNotConfigured() throws ServletException, IOException {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/internal/report");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean called = new AtomicBoolean(false);

        FilterChain chain = (req, res) -> called.set(true);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(called).isFalse();
    }

    @Test
    void returnsForbiddenWhenKeyDoesNotMatch() throws ServletException, IOException {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("expected");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/internal/report");
        request.addHeader("X-INTERNAL-KEY", "other");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean called = new AtomicBoolean(false);

        FilterChain chain = (req, res) -> called.set(true);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(called).isFalse();
    }

    @Test
    void allowsRequestWhenKeyMatches() throws ServletException, IOException {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("expected");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/internal/report");
        request.addHeader("X-INTERNAL-KEY", "expected");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean called = new AtomicBoolean(false);

        FilterChain chain = (req, res) -> called.set(true);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isNotEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(called).isTrue();
    }
}
