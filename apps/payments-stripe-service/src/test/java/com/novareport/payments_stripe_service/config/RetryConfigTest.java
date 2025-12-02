package com.novareport.payments_stripe_service.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RetryConfigTest {

    @Test
    void retryConfigCanBeInstantiated() {
        RetryConfig config = new RetryConfig();
        assertThat(config).isNotNull();
    }
}
