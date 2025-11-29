package com.novareport.notifications_service.client;

import com.novareport.notifications_service.dto.ReportEmailSubscriberResponse;
import com.novareport.notifications_service.dto.UserContactResponse;
import com.novareport.notifications_service.util.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
            List<ReportEmailSubscriberResponse> result = webClient
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
                    .block();

            return result != null ? result : Collections.emptyList();
        } catch (RuntimeException ex) {
            log.warn("Failed to fetch report email subscribers: {}", LogSanitizer.sanitize(ex.getMessage()));
            return Collections.emptyList();
        }
    }

    public UserContactResponse getUserContact(String baseUrl, String internalKey, UUID userId) {
        String correlationId = MDC.get("correlationId");
        try {
            UserContactResponse result = webClient
                .get()
                .uri(baseUrl + "/api/accounts/internal/users/" + userId + "/profile")
                .header("X-INTERNAL-KEY", internalKey)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .headers(headers -> {
                    if (correlationId != null && !correlationId.isBlank()) {
                        headers.set("X-Correlation-ID", correlationId);
                    }
                })
                .retrieve()
                .bodyToMono(UserContactResponse.class)
                .block();

            return result;
        } catch (RuntimeException ex) {
            log.warn(
                "Failed to fetch user contact for {}: {}",
                LogSanitizer.sanitize(userId),
                LogSanitizer.sanitize(ex.getMessage())
            );
            return null;
        }
    }
}
