package com.novareport.notifications_service.client;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscordClientTest {

    @Test
    void sendMessageRejectsNullContent() {
        DiscordClient client = new DiscordClient(WebClient.builder().build());

        assertThatThrownBy(() -> client.sendMessage("https://discord.test/webhook", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("content must not be null");
    }

    @Test
    void sendPayloadRejectsNullArguments() {
        DiscordClient client = new DiscordClient(WebClient.builder().build());

        assertThatThrownBy(() -> client.sendPayload(null, Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("webhookUrl must not be null");

        assertThatThrownBy(() -> client.sendPayload("https://discord.test/webhook", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("body must not be null");
    }

    @Test
    void sendPayloadCompletesOn2xxResponse() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.NO_CONTENT)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .build()
                ))
                .build();

        DiscordClient client = new DiscordClient(webClient);

        MDC.put("correlationId", "corr-discord-ok");
        try {
            client.sendPayload("https://discord.test/webhook", Map.of("content", "hello"))
                    .block();
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Test
    void sendPayloadEmitsErrorOnNon2xxResponse() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.BAD_REQUEST)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                                .body("oops")
                                .build()
                ))
                .build();

        DiscordClient client = new DiscordClient(webClient);

        assertThatThrownBy(() ->
                client.sendPayload("https://discord.test/webhook", Map.of("content", "hello")).block()
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Discord webhook returned 400 BAD_REQUEST");
    }
}
