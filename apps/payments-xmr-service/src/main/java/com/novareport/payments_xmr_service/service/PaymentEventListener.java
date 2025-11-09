package com.novareport.payments_xmr_service.service;

import com.novareport.payments_xmr_service.util.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens to payment events and handles subscription activation outside transaction boundaries.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final SubscriptionActivationService subscriptionActivationService;

    /**
     * Handles payment confirmed events after the transaction has committed.
     * This ensures subscription activation happens outside the payment confirmation transaction.
     * Retries up to 3 times with exponential backoff if activation fails.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2),
        retryFor = {org.springframework.web.client.RestClientException.class, java.io.IOException.class}
    )
    public void handlePaymentConfirmed(PaymentEventPublisher.PaymentConfirmedEvent event) {
        log.info("Handling payment confirmed event for payment {}", LogSanitizer.sanitize(event.payment().getId()));
        subscriptionActivationService.activateSubscriptionForPayment(event.payment());
    }
}
