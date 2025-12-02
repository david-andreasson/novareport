package com.novareport.payments_stripe_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class RestTemplateConfigTest {

    @Test
    void restTemplateIsCreatedFromBuilder() {
        RestTemplateConfig config = new RestTemplateConfig();
        ReflectionTestUtils.setField(config, "connectTimeoutSeconds", 1);
        ReflectionTestUtils.setField(config, "readTimeoutSeconds", 2);

        RestTemplate restTemplate = config.restTemplate(new RestTemplateBuilder());

        assertThat(restTemplate).isNotNull();
    }
}
