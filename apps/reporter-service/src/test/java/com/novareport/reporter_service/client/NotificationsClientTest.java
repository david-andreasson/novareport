package com.novareport.reporter_service.client;

import com.novareport.reporter_service.dto.ReportReadyNotificationRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("null")
class NotificationsClientTest {

    @AfterEach
    void tearDown() {
        MDC.remove("correlationId");
    }

    @Test
    void notifyReportReadyRejectsNullPayload() {
        NotificationsClient client = new NotificationsClient(WebClient.builder().build());

        assertThatThrownBy(() -> client.notifyReportReady("http://notif", "key", null).block())
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void notifyReportReadySendsRequestWithHeadersAndCorrelationId() {
        AtomicReference<ClientRequest> captured = new AtomicReference<>();

        WebClient webClient = WebClient.builder()
            .exchangeFunction(request -> {
                captured.set(request);
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build());
            })
            .build();

        NotificationsClient client = new NotificationsClient(webClient);

        MDC.put("correlationId", "corr-123");

        ReportReadyNotificationRequest payload = new ReportReadyNotificationRequest(
            UUID.randomUUID(),
            LocalDate.of(2024, 1, 1),
            "summary"
        );

        client.notifyReportReady("http://notif", "internal-key", payload).block();

        ClientRequest request = captured.get();
        assertThat(request).isNotNull();
        assertThat(request.url().toString())
            .isEqualTo("http://notif/api/v1/internal/notifications/report-ready");
        assertThat(request.headers().getFirst("X-INTERNAL-KEY")).isEqualTo("internal-key");
        assertThat(request.headers().getFirst(HttpHeaders.CONTENT_TYPE))
            .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(request.headers().getFirst("X-Correlation-ID"))
            .isEqualTo("corr-123");
    }
}
