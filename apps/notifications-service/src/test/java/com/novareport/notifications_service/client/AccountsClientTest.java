package com.novareport.notifications_service.client;

import com.novareport.notifications_service.dto.ReportEmailSubscriberResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountsClientTest {

    @Test
    void getReportEmailSubscribersReturnsListOnSuccess() {
        UUID userId = UUID.randomUUID();
        String body = "[{\"userId\":\"" + userId + "\",\"email\":\"user@example.com\"}]";

        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .body(body)
                                .build()
                ))
                .build();

        AccountsClient client = new AccountsClient(webClient);

        MDC.put("correlationId", "corr-123");
        try {
            List<ReportEmailSubscriberResponse> result =
                    client.getReportEmailSubscribers("http://accounts", "internal-key");

            assertThat(result).hasSize(1);
            ReportEmailSubscriberResponse subscriber = result.get(0);
            assertThat(subscriber.userId()).isEqualTo(userId);
            assertThat(subscriber.email()).isEqualTo("user@example.com");
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Test
    void getReportEmailSubscribersReturnsEmptyListOnError() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.error(new RuntimeException("boom")))
                .build();

        AccountsClient client = new AccountsClient(webClient);

        List<ReportEmailSubscriberResponse> result =
                client.getReportEmailSubscribers("http://accounts", "internal-key");

        assertThat(result).isEmpty();
    }
}
