package com.novareport.payments_stripe_service.service;

import com.novareport.payments_stripe_service.domain.Payment;
import com.novareport.payments_stripe_service.util.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishPaymentConfirmed(Payment payment) {
        log.debug("Publishing payment confirmed event for payment {}", LogSanitizer.sanitize(payment.getId()));
        eventPublisher.publishEvent(new PaymentConfirmedEvent(payment));
    }

    public record PaymentConfirmedEvent(Payment payment) {}
}
