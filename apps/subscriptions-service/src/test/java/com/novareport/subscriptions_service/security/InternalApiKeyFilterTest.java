package com.novareport.subscriptions_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class InternalApiKeyFilterTest {

    @Test
    void shouldNotFilterReturnsTrueForNonInternalPaths() {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/subscriptions/me");

        boolean result = filter.shouldNotFilter(request);

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotFilterReturnsFalseForInternalPaths() {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/internal/subscriptions");

        boolean result = filter.shouldNotFilter(request);

        assertThat(result).isFalse();
    }

    @Test
    void doFilterInternalReturnsForbiddenWhenKeyNotConfigured() throws ServletException, IOException {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/internal/subscriptions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean called = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> called.set(true);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(called).isFalse();
    }

    @Test
    void doFilterInternalReturnsForbiddenWhenKeyDoesNotMatch() throws ServletException, IOException {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("expected-key");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/internal/subscriptions");
        request.addHeader("X-INTERNAL-KEY", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean called = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> called.set(true);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(called).isFalse();
    }

    @Test
    void doFilterInternalDelegatesWhenKeyMatches() throws ServletException, IOException {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("expected-key");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/internal/subscriptions");
        request.addHeader("X-INTERNAL-KEY", "expected-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean called = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> called.set(true);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(called).isTrue();
    }
}
