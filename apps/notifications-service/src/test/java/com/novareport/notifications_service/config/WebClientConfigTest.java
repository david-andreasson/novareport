package com.novareport.notifications_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class WebClientConfigTest {

    private final WebClientConfig config = new WebClientConfig();

    @Test
    void webClientCreatesInstanceWithTimeouts() {
        WebClient client = config.webClient(1L, 2L);

        assertThat(client).isNotNull();
    }
}
