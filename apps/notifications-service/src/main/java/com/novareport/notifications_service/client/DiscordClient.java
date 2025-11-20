package com.novareport.notifications_service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Map;

@Component
public class DiscordClient {

    private static final Logger log = LoggerFactory.getLogger(DiscordClient.class);

    private final WebClient webClient;

    public DiscordClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Void> sendMessage(String webhookUrl, String content) {
        Objects.requireNonNull(content, "content must not be null");
        Map<String, Object> body = Map.of("content", content);
        return sendPayload(webhookUrl, body);
    }

    public Mono<Void> sendPayload(String webhookUrl, Map<String, Object> body) {
        Objects.requireNonNull(webhookUrl, "webhookUrl must not be null");
        Objects.requireNonNull(body, "body must not be null");

        String correlationId = MDC.get("correlationId");

        log.debug("Sending Discord payload with keys={}", body.keySet());

        return webClient
            .post()
            .uri(webhookUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .headers(headers -> {
                if (correlationId != null && !correlationId.isBlank()) {
                    headers.set("X-Correlation-ID", correlationId);
                }
            })
            .bodyValue(body)
            .exchangeToMono(response -> {
                if (response.statusCode().is2xxSuccessful()) {
                    return response.bodyToMono(Void.class)
                        .doOnSuccess(unused -> log.info("Sent Discord message, status={}", response.statusCode()));
                }
                return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(errorBody -> {
                        log.warn("Failed to send Discord message, status={}, body={}", response.statusCode(), errorBody);
                        return Mono.error(new IllegalStateException("Discord webhook returned " + response.statusCode()));
                    });
            });
    }
}
