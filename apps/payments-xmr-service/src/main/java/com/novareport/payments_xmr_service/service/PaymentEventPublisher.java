package com.novareport.payments_xmr_service.service;

import com.novareport.payments_xmr_service.domain.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes payment-related events for asynchronous processing outside transaction boundaries.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Publishes a payment confirmed event that will be handled after transaction commit.
     */
    public void publishPaymentConfirmed(Payment payment) {
        log.debug("Publishing payment confirmed event for payment {}", payment.getId());
        eventPublisher.publishEvent(new PaymentConfirmedEvent(payment));
    }

    /**
     * Event representing a confirmed payment that needs subscription activation.
     */
    public record PaymentConfirmedEvent(Payment payment) {}
}
