package com.novareport.notifications_service.service;

import com.novareport.notifications_service.util.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Service
public class SubscriptionAccessService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionAccessService.class);

    private final WebClient webClient;
    private final String subscriptionsBaseUrl;

    public SubscriptionAccessService(
        WebClient webClient,
        @Value("${subs.base-url:http://subscriptions-service:8080}") String subscriptionsBaseUrl
    ) {
        this.webClient = webClient;
        this.subscriptionsBaseUrl = subscriptionsBaseUrl;
    }

    public boolean hasAccess(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }

        try {
            HasAccessResponse response = webClient
                .get()
                .uri(subscriptionsBaseUrl + "/api/v1/subscriptions/me/has-access")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .retrieve()
                .bodyToMono(HasAccessResponse.class)
                .block(Duration.ofSeconds(5));

            return response != null && response.hasAccess();
        } catch (WebClientResponseException ex) {
            log.warn(
                "Subscription access check failed with status {}: {}",
                ex.getStatusCode(),
                LogSanitizer.sanitize(ex.getMessage())
            );
            if (ex.getStatusCode().is4xxClientError()) {
                return false;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to verify subscription access");
        } catch (Exception ex) {
            log.error(
                "Subscription access check failed: {}",
                LogSanitizer.sanitize(ex.getMessage()),
                ex
            );
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to verify subscription access");
        }
    }

    public record HasAccessResponse(boolean hasAccess) {
    }
}
