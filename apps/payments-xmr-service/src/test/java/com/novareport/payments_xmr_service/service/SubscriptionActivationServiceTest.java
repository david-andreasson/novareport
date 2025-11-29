package com.novareport.payments_xmr_service.service;

import com.novareport.payments_xmr_service.domain.Payment;
import com.novareport.payments_xmr_service.domain.PaymentRepository;
import com.novareport.payments_xmr_service.domain.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private Payment confirmedPayment;

    @BeforeEach
    void setUp() {
        confirmedPayment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .paymentAddress("address")
                .amountXmr(BigDecimal.ONE)
                .plan("monthly")
                .durationDays(30)
                .status(PaymentStatus.CONFIRMED)
                .build();
    }

    @Test
    void activateSubscriptionForPaymentCallsSubscriptionsClientOnSuccess() {
        subscriptionActivationService.activateSubscriptionForPayment(confirmedPayment);

        verify(subscriptionsClient).activateSubscription(
                confirmedPayment.getUserId(),
                confirmedPayment.getPlan(),
                confirmedPayment.getDurationDays()
        );
    }

    @Test
    void activateSubscriptionForPaymentMarksPaymentFailedWhenActivationThrowsSubscriptionActivationException() {
        UUID paymentId = confirmedPayment.getId();

        doThrow(new SubscriptionActivationException("failure"))
                .when(subscriptionsClient)
                .activateSubscription(confirmedPayment.getUserId(), confirmedPayment.getPlan(), confirmedPayment.getDurationDays());

        when(paymentRepository.findByIdWithLock(paymentId)).thenReturn(Optional.of(confirmedPayment));

        subscriptionActivationService.activateSubscriptionForPayment(confirmedPayment);

        verify(paymentRepository).findByIdWithLock(paymentId);
        verify(paymentRepository).save(confirmedPayment);
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
