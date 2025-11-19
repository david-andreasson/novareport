package com.novareport.notifications_service.client;

import com.novareport.notifications_service.dto.ReportEmailSubscriberResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class AccountsClient {

    private static final Logger log = LoggerFactory.getLogger(AccountsClient.class);

    private final WebClient webClient;

    public AccountsClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<ReportEmailSubscriberResponse> getReportEmailSubscribers(String baseUrl, String internalKey) {
        String correlationId = MDC.get("correlationId");
        try {
            return Objects.requireNonNull(webClient
                    .get()
                    .uri(baseUrl + "/api/accounts/internal/report-email-subscribers")
                    .header("X-INTERNAL-KEY", internalKey)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .headers(headers -> {
                        if (correlationId != null && !correlationId.isBlank()) {
                            headers.set("X-Correlation-ID", correlationId);
                        }
                    })
                    .retrieve()
                    .bodyToFlux(ReportEmailSubscriberResponse.class)
                    .collectList()
                    .block());
        } catch (RuntimeException ex) {
            log.warn("Failed to fetch report email subscribers: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }
}
