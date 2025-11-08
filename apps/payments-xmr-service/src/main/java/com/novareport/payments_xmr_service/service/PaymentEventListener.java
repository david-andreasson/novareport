package com.novareport.payments_xmr_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentConfirmed(PaymentEventPublisher.PaymentConfirmedEvent event) {
        log.info("Handling payment confirmed event for payment {}", event.payment().getId());
        subscriptionActivationService.activateSubscriptionForPayment(event.payment());
    }
}
