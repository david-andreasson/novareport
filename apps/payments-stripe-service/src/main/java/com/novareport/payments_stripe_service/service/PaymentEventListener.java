package com.novareport.payments_stripe_service.service;

import com.novareport.payments_stripe_service.util.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final SubscriptionActivationService subscriptionActivationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentConfirmed(PaymentEventPublisher.PaymentConfirmedEvent event) {
        log.info("Handling payment confirmed event for payment {}", LogSanitizer.sanitize(event.payment().getId()));
        subscriptionActivationService.activateSubscriptionForPayment(event.payment());
    }
}
