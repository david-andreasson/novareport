package com.novareport.payments_stripe_service.service;

import com.novareport.payments_stripe_service.domain.Payment;
import com.novareport.payments_stripe_service.domain.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
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
                .stripePaymentIntentId("pi_test_123")
                .amountFiat(49_00L)
                .currencyFiat("SEK")
                .plan("monthly")
                .durationDays(30)
                .status(PaymentStatus.CONFIRMED)
                .build();

        PaymentEventPublisher.PaymentConfirmedEvent event = new PaymentEventPublisher.PaymentConfirmedEvent(payment);

        paymentEventListener.handlePaymentConfirmed(event);

        verify(subscriptionActivationService).activateSubscriptionForPayment(payment);
    }
}
