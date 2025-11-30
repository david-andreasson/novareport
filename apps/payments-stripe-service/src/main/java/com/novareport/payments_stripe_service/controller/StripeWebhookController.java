package com.novareport.payments_stripe_service.controller;

import com.novareport.payments_stripe_service.service.PaymentService;
import com.novareport.payments_stripe_service.util.LogSanitizer;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/payments-stripe/webhook")
@Slf4j
public class StripeWebhookController {

    private final PaymentService paymentService;
    private final String webhookSecret;

    public StripeWebhookController(PaymentService paymentService,
                                   @Value("${stripe.webhook-secret:}") String webhookSecret) {
        this.paymentService = paymentService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping(path = "/stripe", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<String> handleStripe(
            @RequestHeader(name = "Stripe-Signature", required = false) String sigHeader,
            @RequestBody byte[] payloadBytes
    ) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("Stripe webhook called but webhook secret is not configured");
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                    .body("Missing stripe.webhook-secret configuration");
        }

        String payload = new String(payloadBytes, StandardCharsets.UTF_8);
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", LogSanitizer.sanitize(e.getMessage()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        String eventType = event.getType();
        log.info("Received Stripe webhook event: {}", LogSanitizer.sanitize(eventType));

        if ("payment_intent.succeeded".equals(eventType) ||
            "payment_intent.payment_failed".equals(eventType) ||
            "payment_intent.canceled".equals(eventType)) {
            try {
                PaymentIntent pi = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElse(null);
                if (pi != null) {
                    handlePaymentIntentEvent(eventType, pi);
                }
            } catch (ClassCastException ex) {
                log.warn("Unexpected payload type in Stripe webhook: {}",
                        LogSanitizer.sanitize(ex.getMessage()));
            }
        }

        return ResponseEntity.ok("ok");
    }

    private void handlePaymentIntentEvent(String eventType, PaymentIntent pi) {
        String paymentIntentId = pi.getId();
        log.info("Handling Stripe PaymentIntent event {} for id {} with status {}",
                LogSanitizer.sanitize(eventType),
                LogSanitizer.sanitize(paymentIntentId),
                LogSanitizer.sanitize(pi.getStatus()));

        switch (eventType) {
            case "payment_intent.succeeded" ->
                    paymentService.confirmPaymentByStripePaymentIntentId(paymentIntentId);
            case "payment_intent.payment_failed", "payment_intent.canceled" ->
                    paymentService.markPaymentFailedByStripePaymentIntentId(paymentIntentId);
            default -> {
                // ignore
            }
        }
    }
}
