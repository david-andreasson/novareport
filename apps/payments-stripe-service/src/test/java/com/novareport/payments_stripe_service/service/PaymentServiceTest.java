package com.novareport.payments_stripe_service.service;

import com.novareport.payments_stripe_service.domain.Payment;
import com.novareport.payments_stripe_service.domain.PaymentRepository;
import com.novareport.payments_stripe_service.domain.PaymentStatus;
import com.novareport.payments_stripe_service.dto.CreatePaymentIntentResponse;
import com.novareport.payments_stripe_service.dto.PaymentStatusResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @Mock
    private StripePaymentClient stripePaymentClient;

    private SimpleMeterRegistry meterRegistry;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        paymentService = new PaymentService(paymentRepository, paymentEventPublisher, meterRegistry, stripePaymentClient);
    }

    @Test
    void createPaymentIntentCreatesPaymentSuccessfully() throws StripeException {
        UUID userId = UUID.randomUUID();
        String plan = "monthly";

        ReflectionTestUtils.setField(paymentService, "stripeSecretKey", "sk_test_123");

        PaymentIntent paymentIntent = org.mockito.Mockito.mock(PaymentIntent.class);
        when(paymentIntent.getId()).thenReturn("pi_test_123");
        when(paymentIntent.getClientSecret()).thenReturn("cs_test_123");
        when(stripePaymentClient.createPaymentIntent(any(PaymentIntentCreateParams.class))).thenReturn(paymentIntent);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(p, "createdAt", Instant.now());
            return p;
        });

        CreatePaymentIntentResponse response = paymentService.createPaymentIntent(userId, plan);

        assertThat(response.paymentId()).isNotNull();
        assertThat(response.clientSecret()).isEqualTo("cs_test_123");
        assertThat(response.amountFiat()).isEqualTo(49_00L);
        assertThat(response.currencyFiat()).isEqualTo("SEK");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getPlan()).isEqualTo("monthly");
        assertThat(saved.getDurationDays()).isEqualTo(30);
        assertThat(saved.getAmountFiat()).isEqualTo(49_00L);
        assertThat(saved.getCurrencyFiat()).isEqualTo("SEK");
    }

    @Test
    void createPaymentIntentThrowsWhenSecretKeyMissing() throws StripeException {
        UUID userId = UUID.randomUUID();

        ReflectionTestUtils.setField(paymentService, "stripeSecretKey", "");

        assertThatThrownBy(() -> paymentService.createPaymentIntent(userId, "monthly"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stripe secret key is not configured");

        verify(stripePaymentClient, never()).createPaymentIntent(any());
    }

    @Test
    void createPaymentIntentThrowsOnUnknownPlan() throws StripeException {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> paymentService.createPaymentIntent(userId, "weekly"))
                .isInstanceOf(InvalidPlanException.class)
                .hasMessageContaining("Unknown plan");

        verify(stripePaymentClient, never()).createPaymentIntent(any());
    }

    @Test
    void getPaymentStatusReturnsResponseWhenPaymentExists() {
        UUID paymentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(60);
        Instant confirmedAt = Instant.now();

        Payment payment = Payment.builder()
                .id(paymentId)
                .userId(userId)
                .stripePaymentIntentId("pi_test_123")
                .amountFiat(49_00L)
                .currencyFiat("SEK")
                .plan("monthly")
                .durationDays(30)
                .status(PaymentStatus.CONFIRMED)
                .createdAt(createdAt)
                .confirmedAt(confirmedAt)
                .build();

        when(paymentRepository.findByIdAndUserId(paymentId, userId)).thenReturn(Optional.of(payment));

        PaymentStatusResponse response = paymentService.getPaymentStatus(paymentId, userId);

        assertThat(response.paymentId()).isEqualTo(paymentId);
        assertThat(response.status()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.confirmedAt()).isEqualTo(confirmedAt);
    }

    @Test
    void getPaymentStatusThrowsWhenPaymentNotFound() {
        UUID paymentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(paymentRepository.findByIdAndUserId(paymentId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentStatus(paymentId, userId))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    void confirmPaymentConfirmsPendingPaymentAndPublishesEvent() {
        UUID paymentId = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(120);

        Payment payment = Payment.builder()
                .id(paymentId)
                .userId(UUID.randomUUID())
                .stripePaymentIntentId("pi_test_123")
                .amountFiat(49_00L)
                .currencyFiat("SEK")
                .plan("monthly")
                .durationDays(30)
                .status(PaymentStatus.PENDING)
                .createdAt(createdAt)
                .build();

        when(paymentRepository.findByIdWithLock(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.confirmPayment(paymentId);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(payment.getConfirmedAt()).isNotNull();

        verify(paymentRepository).save(payment);
        verify(paymentEventPublisher).publishPaymentConfirmed(payment);
    }

    @Test
    void confirmPaymentThrowsWhenPaymentNotPending() {
        UUID paymentId = UUID.randomUUID();

        Payment payment = Payment.builder()
                .id(paymentId)
                .userId(UUID.randomUUID())
                .stripePaymentIntentId("pi_test_123")
                .amountFiat(49_00L)
                .currencyFiat("SEK")
                .plan("monthly")
                .durationDays(30)
                .status(PaymentStatus.CONFIRMED)
                .createdAt(Instant.now())
                .build();

        when(paymentRepository.findByIdWithLock(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirmPayment(paymentId))
                .isInstanceOf(InvalidPaymentStateException.class)
                .hasMessageContaining("Payment is not in pending state");
    }

    @Test
    void confirmPaymentThrowsWhenPaymentNotFound() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findByIdWithLock(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPayment(paymentId))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    void confirmPaymentByStripePaymentIntentIdDelegatesToConfirmPayment() {
        UUID paymentId = UUID.randomUUID();
        String paymentIntentId = "pi_test_123";

        Payment payment = Payment.builder()
                .id(paymentId)
                .userId(UUID.randomUUID())
                .stripePaymentIntentId(paymentIntentId)
                .amountFiat(49_00L)
                .currencyFiat("SEK")
                .plan("monthly")
                .durationDays(30)
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now().minusSeconds(60))
                .build();

        when(paymentRepository.findByStripePaymentIntentId(paymentIntentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.findByIdWithLock(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.confirmPaymentByStripePaymentIntentId(paymentIntentId);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        verify(paymentRepository).save(payment);
    }

    @Test
    void markPaymentFailedByStripePaymentIntentIdMarksPendingPaymentAsFailed() {
        String paymentIntentId = "pi_test_123";

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .stripePaymentIntentId(paymentIntentId)
                .amountFiat(49_00L)
                .currencyFiat("SEK")
                .plan("monthly")
                .durationDays(30)
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        when(paymentRepository.findByStripePaymentIntentId(paymentIntentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.markPaymentFailedByStripePaymentIntentId(paymentIntentId);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(payment);
    }

    @Test
    void markPaymentFailedByStripePaymentIntentIdDoesNothingWhenNotPending() {
        String paymentIntentId = "pi_test_123";

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .stripePaymentIntentId(paymentIntentId)
                .amountFiat(49_00L)
                .currencyFiat("SEK")
                .plan("monthly")
                .durationDays(30)
                .status(PaymentStatus.CONFIRMED)
                .createdAt(Instant.now())
                .build();

        when(paymentRepository.findByStripePaymentIntentId(paymentIntentId)).thenReturn(Optional.of(payment));

        paymentService.markPaymentFailedByStripePaymentIntentId(paymentIntentId);

        verify(paymentRepository, never()).save(any(Payment.class));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    }
}
