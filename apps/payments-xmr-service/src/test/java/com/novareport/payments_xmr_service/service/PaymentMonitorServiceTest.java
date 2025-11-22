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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class PaymentMonitorServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MoneroWalletClient moneroWalletClient;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentMonitorService paymentMonitorService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentMonitorService, "fakeMode", false);
        ReflectionTestUtils.setField(paymentMonitorService, "monitorEnabled", true);
    }

    @Test
    void checkPendingPaymentsDoesNothingWhenFakeModeIsEnabled() {
        ReflectionTestUtils.setField(paymentMonitorService, "fakeMode", true);

        paymentMonitorService.checkPendingPayments();

        verify(paymentRepository, never()).findByStatus(any());
    }

    @Test
    void checkPendingPaymentsDoesNothingWhenMonitorDisabled() {
        ReflectionTestUtils.setField(paymentMonitorService, "monitorEnabled", false);

        paymentMonitorService.checkPendingPayments();

        verify(paymentRepository, never()).findByStatus(any());
    }

    @Test
    void checkPendingPaymentsDoesNothingWhenNoPendingPayments() {
        when(paymentRepository.findByStatus(PaymentStatus.PENDING)).thenReturn(Collections.emptyList());

        paymentMonitorService.checkPendingPayments();

        verify(moneroWalletClient, never()).getConfirmedBalanceForSubaddress(any(Integer.class), any(Integer.class));
        verify(paymentService, never()).confirmPayment(any(UUID.class));
    }

    @Test
    void checkPendingPaymentsSkipsPaymentsWithoutWalletIndices() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .paymentAddress("address")
                .amountXmr(BigDecimal.ONE)
                .plan("monthly")
                .durationDays(30)
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .walletAccountIndex(null)
                .walletSubaddressIndex(null)
                .build();

        when(paymentRepository.findByStatus(PaymentStatus.PENDING)).thenReturn(List.of(payment));

        paymentMonitorService.checkPendingPayments();

        verify(moneroWalletClient, never()).getConfirmedBalanceForSubaddress(any(Integer.class), any(Integer.class));
        verify(paymentService, never()).confirmPayment(any(UUID.class));
    }

    @Test
    void checkPendingPaymentsConfirmsPaymentWhenBalanceIsSufficient() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId)
                .userId(UUID.randomUUID())
                .paymentAddress("address")
                .amountXmr(BigDecimal.valueOf(1.0))
                .plan("monthly")
                .durationDays(30)
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .walletAccountIndex(0)
                .walletSubaddressIndex(5)
                .build();

        when(paymentRepository.findByStatus(PaymentStatus.PENDING)).thenReturn(List.of(payment));
        when(moneroWalletClient.getConfirmedBalanceForSubaddress(0, 5)).thenReturn(BigDecimal.valueOf(1.5));

        paymentMonitorService.checkPendingPayments();

        verify(paymentService).confirmPayment(paymentId);
    }
}
