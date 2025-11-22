package com.novareport.payments_xmr_service.service;

import com.novareport.payments_xmr_service.domain.Payment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    private SubscriptionActivationService subscriptionActivationService;

    @InjectMocks
    private PaymentEventListener paymentEventListener;

    @Test
    void handlePaymentConfirmedDelegatesToSubscriptionActivationService() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .paymentAddress("address")
                .amountXmr(BigDecimal.ONE)
                .plan("monthly")
                .durationDays(30)
                .createdAt(Instant.now())
                .build();

        PaymentEventPublisher.PaymentConfirmedEvent event = new PaymentEventPublisher.PaymentConfirmedEvent(payment);

        paymentEventListener.handlePaymentConfirmed(event);

        verify(subscriptionActivationService).activateSubscriptionForPayment(payment);
    }
}
