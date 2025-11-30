package com.novareport.payments_stripe_service.service;

import com.novareport.payments_stripe_service.domain.Payment;
import com.novareport.payments_stripe_service.dto.PaymentConfirmedEmailRequest;
import com.novareport.payments_stripe_service.util.LogSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;

@Service
@Slf4j
public class NotificationsClient {

    private final RestTemplate restTemplate;
    private final String notificationsBaseUrl;
    private final String internalApiKey;

    public NotificationsClient(
            RestTemplate restTemplate,
            @Value("${notifications.base-url}") String notificationsBaseUrl,
            @Value("${internal.api-key}") String internalApiKey
    ) {
        this.restTemplate = restTemplate;
        this.notificationsBaseUrl = notificationsBaseUrl;
        this.internalApiKey = internalApiKey;
    }

    private void validateBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("Notifications base URL is not configured");
        }
        if (!baseUrl.startsWith("http://localhost") &&
            !baseUrl.startsWith("http://notifications-service") &&
            !baseUrl.startsWith("https://notifications-service")) {
            throw new IllegalStateException("Invalid notifications base URL: " + baseUrl);
        }
    }

    public void sendPaymentConfirmedEmail(Payment payment) {
        if (payment == null) {
            return;
        }

        if (internalApiKey == null || internalApiKey.isBlank()) {
            log.warn("Internal API key is not configured; skipping payment confirmation email for payment {}", LogSanitizer.sanitize(payment.getId()));
            return;
        }

        String baseUrl = Objects.requireNonNull(notificationsBaseUrl, "Notifications base URL is not configured");
        validateBaseUrl(baseUrl);

        PaymentConfirmedEmailRequest request = new PaymentConfirmedEmailRequest(
                payment.getUserId(),
                payment.getPlan(),
                payment.getDurationDays()
        );

        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/v1/internal/notifications/payment-confirmed-email")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-INTERNAL-KEY", internalApiKey);
        String correlationId = MDC.get("correlationId");
        if (correlationId != null && !correlationId.isBlank()) {
            headers.set("X-Correlation-ID", correlationId);
        }

        HttpEntity<PaymentConfirmedEmailRequest> entity = new HttpEntity<>(request, headers);

        try {
            var response = restTemplate.exchange(
                    url,
                    Objects.requireNonNull(HttpMethod.POST, "HTTP method must not be null"),
                    entity,
                    Void.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Failed to send payment confirmation email for payment {}. Unexpected response status: {}",
                        LogSanitizer.sanitize(payment.getId()), response.getStatusCode());
            } else {
                log.info("Successfully requested payment confirmation email for payment {}", LogSanitizer.sanitize(payment.getId()));
            }
        } catch (RestClientException ex) {
            log.warn("Error calling notifications-service for payment confirmation email for payment {}: {}",
                    LogSanitizer.sanitize(payment.getId()), LogSanitizer.sanitize(ex.getMessage()));
        } catch (RuntimeException ex) {
            log.warn("Unexpected error when sending payment confirmation email for payment {}: {}",
                    LogSanitizer.sanitize(payment.getId()), LogSanitizer.sanitize(ex.getMessage()));
        }
    }
}
