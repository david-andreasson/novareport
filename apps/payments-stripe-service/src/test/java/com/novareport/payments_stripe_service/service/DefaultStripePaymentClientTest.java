package com.novareport.payments_stripe_service.service;

import com.stripe.exception.StripeException;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultStripePaymentClientTest {

    private DefaultStripePaymentClient client;

    @BeforeEach
    void setUp() {
        client = new DefaultStripePaymentClient();
    }

    @Test
    void createPaymentIntentDelegatesToStripeApi() {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(1000L)
                .setCurrency("usd")
                .build();

        assertThatThrownBy(() -> client.createPaymentIntent(params))
                .isInstanceOf(StripeException.class);
    }
}
