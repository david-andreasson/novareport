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

@Component
public class DiscordClient {

    private static final Logger log = LoggerFactory.getLogger(DiscordClient.class);

    private final WebClient webClient;

    public DiscordClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Void> sendMessage(String webhookUrl, String content) {
        Objects.requireNonNull(webhookUrl, "webhookUrl must not be null");
        Objects.requireNonNull(content, "content must not be null");

        String correlationId = MDC.get("correlationId");

        return webClient
            .post()
            .uri(webhookUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .headers(headers -> {
                if (correlationId != null && !correlationId.isBlank()) {
                    headers.set("X-Correlation-ID", correlationId);
                }
            })
            .bodyValue("{" + "\"content\": " + toJsonString(content) + "}")
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSuccess(unused -> log.info("Sent Discord message"))
            .doOnError(ex -> log.warn("Failed to send Discord message: {}", ex.getMessage()));
    }

    private String toJsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
