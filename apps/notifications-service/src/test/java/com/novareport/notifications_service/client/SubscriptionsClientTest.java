package com.novareport.notifications_service.client;

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

class SubscriptionsClientTest {

    @Test
    void getActiveUserIdsReturnsListOnSuccess() {
        UUID userId = UUID.randomUUID();
        String body = "{\"userIds\":[\"" + userId + "\"]}";

        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .body(body)
                                .build()
                ))
                .build();

        SubscriptionsClient client = new SubscriptionsClient(webClient);

        MDC.put("correlationId", "corr-456");
        try {
            List<UUID> userIds = client.getActiveUserIds("http://subs", "internal-key");

            assertThat(userIds).containsExactly(userId);
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Test
    void getActiveUserIdsReturnsEmptyListOnError() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.error(new RuntimeException("boom")))
                .build();

        SubscriptionsClient client = new SubscriptionsClient(webClient);

        List<UUID> userIds = client.getActiveUserIds("http://subs", "internal-key");

        assertThat(userIds).isEmpty();
    }
}
