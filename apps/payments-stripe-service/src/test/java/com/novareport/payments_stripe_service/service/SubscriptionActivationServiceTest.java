package com.novareport.payments_stripe_service.service;

import com.novareport.payments_stripe_service.domain.Payment;
import com.novareport.payments_stripe_service.domain.PaymentRepository;
import com.novareport.payments_stripe_service.domain.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class SubscriptionActivationServiceTest {

    @Mock
    private SubscriptionsClient subscriptionsClient;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private NotificationsClient notificationsClient;

    @InjectMocks
    private SubscriptionActivationService subscriptionActivationService;

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .stripePaymentIntentId("pi_test_123")
                .plan("monthly")
                .durationDays(30)
                .amountFiat(49_00L)
                .currencyFiat("SEK")
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Test
    void activateSubscriptionForPaymentCallsSubscriptionsClientAndNotifications() {
        subscriptionActivationService.activateSubscriptionForPayment(payment);

        verify(subscriptionsClient).activateSubscription(payment.getUserId(), payment.getPlan(), payment.getDurationDays());
        verify(notificationsClient).sendPaymentConfirmedEmail(payment);
    }

    @Test
    void activateSubscriptionForPaymentMarksPaymentFailedOnSubscriptionActivationException() {
        doThrow(new SubscriptionActivationException("failure"))
                .when(subscriptionsClient)
                .activateSubscription(payment.getUserId(), payment.getPlan(), payment.getDurationDays());

        when(paymentRepository.findByIdWithLock(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        subscriptionActivationService.activateSubscriptionForPayment(payment);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(payment);
    }

    @Test
    void markPaymentAsFailedThrowsWhenPaymentNotFound() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findByIdWithLock(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionActivationService.markPaymentAsFailed(paymentId))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found");
    }
}
