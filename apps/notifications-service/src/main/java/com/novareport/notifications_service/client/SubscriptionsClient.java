package com.novareport.notifications_service.client;

import com.novareport.notifications_service.dto.ActiveSubscriptionUsersResponse;
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
public class SubscriptionsClient {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionsClient.class);

    private final WebClient webClient;

    public SubscriptionsClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<UUID> getActiveUserIds(String baseUrl, String internalKey) {
        String correlationId = MDC.get("correlationId");
        try {
            ActiveSubscriptionUsersResponse response = webClient
                    .get()
                    .uri(baseUrl + "/api/v1/internal/subscriptions/active-users")
                    .header("X-INTERNAL-KEY", internalKey)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .headers(headers -> {
                        if (correlationId != null && !correlationId.isBlank()) {
                            headers.set("X-Correlation-ID", correlationId);
                        }
                    })
                    .retrieve()
                    .bodyToMono(ActiveSubscriptionUsersResponse.class)
                    .block();

            if (response == null || response.userIds() == null) {
                return Collections.emptyList();
            }
            return response.userIds();
        } catch (RuntimeException ex) {
            log.warn("Failed to fetch active subscription user ids: {}", LogSanitizer.sanitize(ex.getMessage()));
            return Collections.emptyList();
        }
    }
}
