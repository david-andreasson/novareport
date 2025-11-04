package com.novareport.reporter_service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Map;

@Component
public class NotificationsClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationsClient.class);

    private final WebClient webClient;

    public NotificationsClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Void> notifyReportReady(String baseUrl, String internalKey, LocalDate date) {
        return webClient
            .post()
            .uri(baseUrl + "/api/v1/internal/notifications/report-ready")
            .header("X-INTERNAL-KEY", internalKey)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(Map.of("date", date.toString()))
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSuccess(unused -> log.info("Sent report-ready notification for {}", date))
            .doOnError(ex -> log.warn("Failed to send report-ready notification for {}: {}", date, ex.getMessage()));
    }
}
