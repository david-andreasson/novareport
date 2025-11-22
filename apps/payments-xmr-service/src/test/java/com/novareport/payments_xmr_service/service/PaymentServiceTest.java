package com.novareport.payments_xmr_service.service;

import com.novareport.payments_xmr_service.domain.Payment;
import com.novareport.payments_xmr_service.domain.PaymentRepository;
import com.novareport.payments_xmr_service.domain.PaymentStatus;
import com.novareport.payments_xmr_service.dto.CreatePaymentResponse;
import com.novareport.payments_xmr_service.dto.PaymentStatusResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
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
    private MoneroWalletClient moneroWalletClient;

    private SimpleMeterRegistry meterRegistry;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        paymentService = new PaymentService(paymentRepository, paymentEventPublisher, moneroWalletClient, meterRegistry);
        // Default to fake mode for most tests
        ReflectionTestUtils.setField(paymentService, "fakeMode", true);
    }

    @Test
    void createPaymentInFakeModeGeneratesFakeAddressAndDoesNotCallMonero() {
        UUID userId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(1.23);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
            return p;
        });

        CreatePaymentResponse response = paymentService.createPayment(userId, "monthly", amount);

        assertThat(response.paymentId()).isNotNull();
        assertThat(response.paymentAddress()).isNotBlank();
        assertThat(response.paymentAddress()).startsWith("4");
        assertThat(response.expiresAt()).isAfter(Instant.now());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertThat(saved.getWalletAccountIndex()).isNull();
        assertThat(saved.getWalletSubaddressIndex()).isNull();

        verify(moneroWalletClient, never()).createSubaddress(any(Integer.class), any(String.class));
    }

    @Test
    void createPaymentInRealModeUsesMoneroWalletClient() {
        UUID userId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(0.5);

        ReflectionTestUtils.setField(paymentService, "fakeMode", false);

        MoneroWalletClient.MoneroSubaddress subaddress = new MoneroWalletClient.MoneroSubaddress(0, 5, "4abc");
        when(moneroWalletClient.createSubaddress(0, "novareport-" + userId)).thenReturn(subaddress);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
            return p;
        });

        CreatePaymentResponse response = paymentService.createPayment(userId, "monthly", amount);

        assertThat(response.paymentId()).isNotNull();
        assertThat(response.paymentAddress()).isEqualTo("4abc");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertThat(saved.getWalletAccountIndex()).isEqualTo(0);
        assertThat(saved.getWalletSubaddressIndex()).isEqualTo(5);
    }

    @Test
    void createPaymentThrowsOnNullPlan() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> paymentService.createPayment(userId, null, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plan cannot be null");
    }

    @Test
    void createPaymentThrowsOnUnknownPlan() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> paymentService.createPayment(userId, "weekly", BigDecimal.ONE))
                .isInstanceOf(InvalidPlanException.class)
                .hasMessageContaining("Unknown plan");
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
                .paymentAddress("address")
                .amountXmr(BigDecimal.ONE)
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
                .paymentAddress("address")
                .amountXmr(BigDecimal.ONE)
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
                .paymentAddress("address")
                .amountXmr(BigDecimal.ONE)
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
}
