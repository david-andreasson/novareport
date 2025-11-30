package com.novareport.payments_stripe_service.service;

import com.novareport.payments_stripe_service.dto.ActivateSubscriptionRequest;
import com.novareport.payments_stripe_service.util.LogSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class SubscriptionsClient {

    private final RestTemplate restTemplate;
    private final String subscriptionsBaseUrl;
    private final String internalApiKey;

    public SubscriptionsClient(
            RestTemplate restTemplate,
            @Value("${subscriptions.base-url}") String subscriptionsBaseUrl,
            @Value("${internal.api-key}") String internalApiKey
    ) {
        this.restTemplate = restTemplate;
        this.subscriptionsBaseUrl = subscriptionsBaseUrl;
        this.internalApiKey = internalApiKey;
    }

    private void validateBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("Subscriptions base URL is not configured");
        }
        if (!baseUrl.startsWith("http://localhost") &&
            !baseUrl.startsWith("http://subscriptions-service") &&
            !baseUrl.startsWith("https://subscriptions-service")) {
            throw new IllegalStateException("Invalid subscriptions base URL: " + baseUrl);
        }
    }

    @Retryable(
        retryFor = {org.springframework.web.client.RestClientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000)
    )
    public void activateSubscription(UUID userId, String plan, int durationDays) {
        String baseUrl = Objects.requireNonNull(subscriptionsBaseUrl, "Subscriptions base URL is not configured");
        validateBaseUrl(baseUrl);

        ActivateSubscriptionRequest request = new ActivateSubscriptionRequest(
                userId,
                plan,
                durationDays
        );

        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/v1/internal/subscriptions/activate")
                .toUriString();

        log.info("Activating subscription for user {} with plan {} for {} days", LogSanitizer.sanitize(userId), LogSanitizer.sanitize(plan), LogSanitizer.sanitize(durationDays));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-INTERNAL-KEY", internalApiKey);
            String correlationId = MDC.get("correlationId");
            if (correlationId != null && !correlationId.isBlank()) {
                headers.set("X-Correlation-ID", correlationId);
            }
            HttpEntity<ActivateSubscriptionRequest> entity = new HttpEntity<>(request, headers);

            var response = restTemplate.exchange(
                    url,
                    Objects.requireNonNull(HttpMethod.POST, "HTTP method must not be null"),
                    entity,
                    Void.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to activate subscription for user {}. Unexpected response status: {}",
                    LogSanitizer.sanitize(userId), response.getStatusCode());
                throw new SubscriptionActivationException("Unexpected response status: " + response.getStatusCode());
            }

            log.info("Successfully activated subscription for user {}. Response status: {}",
                LogSanitizer.sanitize(userId), response.getStatusCode());
        } catch (org.springframework.web.client.RestClientException e) {
            log.error("Failed to activate subscription for user {}: {}", LogSanitizer.sanitize(userId), LogSanitizer.sanitize(e.getMessage()));
            throw new SubscriptionActivationException("Failed to activate subscription", e);
        }
    }
}
