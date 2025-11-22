package com.novareport.notifications_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generatesCorrelationIdWhenMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            String inMdc = MDC.get("correlationId");
            assertThat(inMdc).isNotBlank();
            assertThat(response.getHeader("X-Correlation-ID")).isEqualTo(inMdc);
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Correlation-ID")).isNotBlank();
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void reusesExistingCorrelationIdHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", "corr-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            assertThat(MDC.get("correlationId")).isEqualTo("corr-123");
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Correlation-ID")).isEqualTo("corr-123");
        assertThat(MDC.get("correlationId")).isNull();
    }
}
