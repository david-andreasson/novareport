package com.novareport.accounts_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generatesCorrelationIdWhenMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/accounts/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            String fromMdc = MDC.get("correlationId");
            assertThat(fromMdc).isNotBlank();
        };

        filter.doFilterInternal(request, response, chain);

        String header = response.getHeader("X-Correlation-ID");
        assertThat(header).isNotBlank();
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void reusesExistingCorrelationIdFromRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/accounts/me");
        request.addHeader("X-Correlation-ID", "existing-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean called = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> called.set(true);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getHeader("X-Correlation-ID")).isEqualTo("existing-id");
        assertThat(called).isTrue();
    }
}
