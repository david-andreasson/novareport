package com.novareport.payments_xmr_service.service;

import com.novareport.payments_xmr_service.dto.ActivateSubscriptionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
public class SubscriptionsClient {

    private final WebClient webClient;
    private final String subscriptionsBaseUrl;
    private final String internalApiKey;

    public SubscriptionsClient(
            WebClient webClient,
            @Value("${subscriptions.base-url}") String subscriptionsBaseUrl,
            @Value("${internal.api-key}") String internalApiKey
    ) {
        this.webClient = webClient;
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

        log.info("Activating subscription for user {} with plan {} for {} days", userId, plan, durationDays);

        try {
            webClient.post()
                    .uri(url)
                    .header("X-INTERNAL-KEY", internalApiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(5))
                    .blockOptional()
                    .orElseThrow(() -> new SubscriptionActivationException("Subscription activation timed out"));

            log.info("Successfully activated subscription for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to activate subscription for user {}: {}", userId, e.getMessage());
            throw new SubscriptionActivationException("Failed to activate subscription", e);
        }
    }
}
