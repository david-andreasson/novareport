package com.novareport.reporter_service.client;

import com.novareport.reporter_service.domain.SubscriptionAccessResponse;
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

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class SubscriptionsClientTest {

    @AfterEach
    void tearDown() {
        MDC.remove("correlationId");
    }

    @Test
    void hasAccessSendsRequestWithHeadersAndParsesResponse() {
        AtomicReference<ClientRequest> captured = new AtomicReference<>();

        String body = "{\"hasAccess\":true}";

        WebClient webClient = WebClient.builder()
            .exchangeFunction(request -> {
                captured.set(request);
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .build());
            })
            .build();

        SubscriptionsClient client = new SubscriptionsClient(webClient);

        MDC.put("correlationId", "corr-456");

        SubscriptionAccessResponse response = client.hasAccess("http://subs", "token").block();

        assertThat(response).isNotNull();
        assertThat(response.hasAccess()).isTrue();

        ClientRequest request = captured.get();
        assertThat(request).isNotNull();
        assertThat(request.url().toString())
            .isEqualTo("http://subs/api/v1/subscriptions/me/has-access");
        assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION))
            .isEqualTo("Bearer token");
        assertThat(request.headers().getFirst("X-Correlation-ID"))
            .isEqualTo("corr-456");
    }
}
