package com.novareport.payments_stripe_service.controller;

import com.novareport.payments_stripe_service.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StripeWebhookControllerTest {

    @Mock
    private PaymentService paymentService;

    @Test
    void handleStripeReturnsPreconditionRequiredWhenSecretMissing() {
        StripeWebhookController controller = new StripeWebhookController(paymentService, "");

        ResponseEntity<String> response = controller.handleStripe(null, "{}".getBytes());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_REQUIRED);
        assertThat(response.getBody()).contains("Missing stripe.webhook-secret");
        verifyNoInteractions(paymentService);
    }

    @Test
    void handleStripeReturnsBadRequestOnInvalidSignature() {
        StripeWebhookController controller = new StripeWebhookController(paymentService, "whsec_test");

        ResponseEntity<String> response = controller.handleStripe("invalid-signature", "{}".getBytes());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Invalid signature");
        verifyNoInteractions(paymentService);
    }

    @Test
    void handleStripeConfirmsPaymentOnSucceededEvent() {
        String secret = "whsec_test";
        StripeWebhookController controller = new StripeWebhookController(paymentService, secret);
        String payload = STRIPE_EVENT_TEMPLATE
                .replace("${TYPE}", "payment_intent.succeeded")
                .replace("${PAYMENT_INTENT_ID}", "pi_succeeded");

        String signature = stripeSignature(secret, payload);

        ResponseEntity<String> response = controller.handleStripe(signature, payload.getBytes(StandardCharsets.UTF_8));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).confirmPaymentByStripePaymentIntentId("pi_succeeded");
        verify(paymentService, never()).markPaymentFailedByStripePaymentIntentId("pi_succeeded");
    }

    @Test
    void handleStripeMarksPaymentFailedOnCanceledEvent() {
        String secret = "whsec_test";
        StripeWebhookController controller = new StripeWebhookController(paymentService, secret);
        String payload = STRIPE_EVENT_TEMPLATE
                .replace("${TYPE}", "payment_intent.canceled")
                .replace("${PAYMENT_INTENT_ID}", "pi_failed");

        String signature = stripeSignature(secret, payload);

        ResponseEntity<String> response = controller.handleStripe(signature, payload.getBytes(StandardCharsets.UTF_8));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).markPaymentFailedByStripePaymentIntentId("pi_failed");
        verify(paymentService, never()).confirmPaymentByStripePaymentIntentId("pi_failed");
    }

    @Test
    void handleStripeSkipsWhenPaymentIntentMissing() {
        String secret = "whsec_test";
        StripeWebhookController controller = new StripeWebhookController(paymentService, secret);
        String payload = "{" +
                "\"id\":\"evt_test\"," +
                "\"object\":\"event\"," +
                "\"type\":\"payment_intent.succeeded\"," +
                "\"data\":{\"object\":{}}" +
                "}";

        String signature = stripeSignature(secret, payload);

        ResponseEntity<String> response = controller.handleStripe(signature, payload.getBytes(StandardCharsets.UTF_8));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verifyNoInteractions(paymentService);
    }

    private static final String STRIPE_EVENT_TEMPLATE = "{" +
            "\"id\":\"evt_test\"," +
            "\"object\":\"event\"," +
            "\"type\":\"${TYPE}\"," +
            "\"data\":{\"object\":{\"id\":\"${PAYMENT_INTENT_ID}\",\"object\":\"payment_intent\"}}" +
            "}";

    private String stripeSignature(String secret, String payload) {
        long timestamp = Instant.now().getEpochSecond();
        String signedPayload = timestamp + "." + payload;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            String signature = HexFormat.of().formatHex(hash);
            return "t=" + timestamp + ",v1=" + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create Stripe signature", e);
        }
    }
}
