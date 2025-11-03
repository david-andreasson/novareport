package com.novareport.reporter_service.service;

import com.novareport.reporter_service.client.SubscriptionsClient;
import com.novareport.reporter_service.domain.SubscriptionAccessResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Service
public class SubscriptionAccessService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionAccessService.class);

    private final SubscriptionsClient subscriptionsClient;
    private final String subscriptionsBaseUrl;

    public SubscriptionAccessService(
        SubscriptionsClient subscriptionsClient,
        @Value("${subs.base-url}") String subscriptionsBaseUrl
    ) {
        this.subscriptionsClient = subscriptionsClient;
        this.subscriptionsBaseUrl = subscriptionsBaseUrl;
    }

    public void assertAccess(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        boolean hasAccess = fetchAccess(token);
        if (!hasAccess) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Subscription required");
        }
    }

    private boolean fetchAccess(String token) {
        try {
            return subscriptionsClient.hasAccess(subscriptionsBaseUrl, token)
                .map(SubscriptionAccessResponse::hasAccess)
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(ex -> {
                    log.warn("Subscription access call failed: {}", ex.getMessage());
                    return switch (ex) {
                        case WebClientResponseException e when e.getStatusCode().is4xxClientError() -> {
                            yield reactor.core.publisher.Mono.just(false);
                        }
                        default -> reactor.core.publisher.Mono.error(ex);
                    };
                })
                .blockOptional()
                .orElse(false);
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                return false;
            }
            log.error("Subscription access check failed with status {}", ex.getStatusCode());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to verify subscription access");
        } catch (Exception ex) {
            log.error("Subscription access check failed", ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to verify subscription access");
        }
    }

    private String extractToken(String header) {
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }
        String token = header.substring(7);
        if (!StringUtils.hasText(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }
        return token;
    }
}
