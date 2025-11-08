package com.novareport.payments_xmr_service.service;

import com.novareport.payments_xmr_service.dto.ActivateSubscriptionRequest;
import com.novareport.payments_xmr_service.util.LogSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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

    public void activateSubscription(UUID userId, String plan, int durationDays) {
        ActivateSubscriptionRequest request = new ActivateSubscriptionRequest(
                userId,
                plan,
                durationDays
        );

        String url = UriComponentsBuilder.fromUriString(subscriptionsBaseUrl)
                .path("/api/v1/internal/subscriptions/activate")
                .toUriString();

        log.info("Activating subscription for user {} with plan {} for {} days", LogSanitizer.sanitize(userId), LogSanitizer.sanitize(plan), LogSanitizer.sanitize(durationDays));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-INTERNAL-KEY", internalApiKey);
            HttpEntity<ActivateSubscriptionRequest> entity = new HttpEntity<>(request, headers);
            
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);

            log.info("Successfully activated subscription for user {}", LogSanitizer.sanitize(userId));
        } catch (Exception e) {
            log.error("Failed to activate subscription for user {}: {}", LogSanitizer.sanitize(userId), LogSanitizer.sanitize(e.getMessage()));
            throw new SubscriptionActivationException("Failed to activate subscription", e);
        }
    }
}
