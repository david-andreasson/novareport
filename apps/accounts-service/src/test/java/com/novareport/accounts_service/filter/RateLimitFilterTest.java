package com.novareport.accounts_service.filter;

import com.novareport.accounts_service.config.RateLimitingConfig;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private Map<String, Bucket> buckets;
    private RateLimitingConfig config;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        buckets = new ConcurrentHashMap<>();
        config = mock(RateLimitingConfig.class);
        filter = new RateLimitFilter(buckets, config);
    }

    @Test
    void allowsRequestWhenBucketHasTokens() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/login");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Bucket bucket = mock(Bucket.class);
        when(config.createBucket()).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        AtomicBoolean called = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> called.set(true);

        filter.doFilterInternal(request, response, chain);

        assertThat(called).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void blocksRequestWhenBucketIsExhausted() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/accounts/auth/login");
        request.setRemoteAddr("192.168.0.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Bucket bucket = mock(Bucket.class);
        when(config.createBucket()).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(false);

        AtomicBoolean called = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> called.set(true);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("För många inloggningsförsök");
        assertThat(called).isFalse();
    }

    @Test
    void bypassesNonAuthPaths() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/accounts/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean called = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> called.set(true);

        filter.doFilterInternal(request, response, chain);

        assertThat(called).isTrue();
        verifyNoInteractions(config);
    }
}
