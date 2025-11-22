package com.novareport.reporter_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class WebClientConfigTest {

    @Test
    void webClientBeanIsCreatedWithTimeouts() {
        WebClientConfig config = new WebClientConfig();

        WebClient client = config.webClient(5L, 10L);

        assertThat(client).isNotNull();
    }
}
