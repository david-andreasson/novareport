package com.novareport.payments_xmr_service.service;

import com.novareport.payments_xmr_service.domain.Payment;
import com.novareport.payments_xmr_service.domain.PaymentRepository;
import com.novareport.payments_xmr_service.domain.PaymentStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class PaymentToSubscriptionFlowTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SubscriptionsClient subscriptionsClient;

    private SubscriptionActivationService subscriptionActivationService;

    private PaymentEventListener paymentEventListener;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        subscriptionActivationService = new SubscriptionActivationService(subscriptionsClient, paymentRepository);
        paymentEventListener = new PaymentEventListener(subscriptionActivationService);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        PaymentEventPublisher testEventPublisher = new PaymentEventPublisher(mock(ApplicationEventPublisher.class)) {
            @Override
            public void publishPaymentConfirmed(Payment payment) {
                paymentEventListener.handlePaymentConfirmed(new PaymentConfirmedEvent(payment));
            }
        };

        paymentService = new PaymentService(paymentRepository, testEventPublisher, mock(MoneroWalletClient.class), meterRegistry);
    }

    @Test
    void confirmPaymentActivatesSubscriptionForUser() {
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(1.23);
        Instant createdAt = Instant.now().minusSeconds(60);

        Payment payment = Payment.builder()
                .id(paymentId)
                .userId(userId)
                .paymentAddress("address")
                .amountXmr(amount)
                .plan("monthly")
                .durationDays(30)
                .status(PaymentStatus.PENDING)
                .createdAt(createdAt)
                .build();

        when(paymentRepository.findByIdWithLock(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.confirmPayment(paymentId);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        verify(paymentRepository).save(payment);
        verify(subscriptionsClient).activateSubscription(userId, "monthly", 30);
    }

    @Test
    void confirmPaymentMarksPaymentFailedWhenSubscriptionActivationFails() {
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(0.5);
        Instant createdAt = Instant.now().minusSeconds(120);

        Payment payment = Payment.builder()
                .id(paymentId)
                .userId(userId)
                .paymentAddress("address")
                .amountXmr(amount)
                .plan("monthly")
                .durationDays(30)
                .status(PaymentStatus.PENDING)
                .createdAt(createdAt)
                .build();

        when(paymentRepository.findByIdWithLock(paymentId))
                .thenReturn(Optional.of(payment))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new SubscriptionActivationException("failure"))
                .when(subscriptionsClient)
                .activateSubscription(userId, "monthly", 30);

        paymentService.confirmPayment(paymentId);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(subscriptionsClient).activateSubscription(userId, "monthly", 30);
        verify(paymentRepository, times(2)).save(payment);
    }
}
