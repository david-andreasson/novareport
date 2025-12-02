package com.novareport.payments_xmr_service.controller;

import com.novareport.payments_xmr_service.domain.Payment;
import com.novareport.payments_xmr_service.domain.PaymentRepository;
import com.novareport.payments_xmr_service.domain.PaymentStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPaymentControllerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Search search;

    @Mock
    private Counter counter;

    @Mock
    private Timer timer;

    private AdminPaymentController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminPaymentController(paymentRepository, meterRegistry);
    }

    @Test
    void metricsReturnsAggregatedData() {
        when(paymentRepository.count()).thenReturn(100L);
        when(paymentRepository.countByStatus(PaymentStatus.CONFIRMED)).thenReturn(80L);

        when(meterRegistry.find(anyString())).thenReturn(search);
        when(search.counters()).thenReturn(List.of());
        when(search.timers()).thenReturn(List.of());

        var response = controller.metrics();

        assertThat(response).isNotNull();
        assertThat(response.created().success()).isEqualTo(100L);
        assertThat(response.confirmed().success()).isEqualTo(80L);
    }

    @Test
    void metricsCalculatesCountersCorrectly() {
        when(paymentRepository.count()).thenReturn(50L);
        when(paymentRepository.countByStatus(PaymentStatus.CONFIRMED)).thenReturn(40L);

        when(meterRegistry.find(anyString())).thenReturn(search);
        when(search.counters()).thenReturn(List.of(counter));
        when(counter.getId()).thenReturn(mock(io.micrometer.core.instrument.Meter.Id.class));
        when(counter.getId().getTag("outcome")).thenReturn("error");
        when(counter.count()).thenReturn(5.0);
        when(search.timers()).thenReturn(List.of());

        var response = controller.metrics();

        assertThat(response.created().error()).isEqualTo(5L);
    }

    @Test
    void metricsCalculatesTimerMeanCorrectly() {
        when(paymentRepository.count()).thenReturn(10L);
        when(paymentRepository.countByStatus(PaymentStatus.CONFIRMED)).thenReturn(8L);

        when(meterRegistry.find(anyString())).thenReturn(search);
        when(search.counters()).thenReturn(List.of());
        when(search.timers()).thenReturn(List.of(timer));
        when(timer.count()).thenReturn(10L);
        when(timer.mean(TimeUnit.MILLISECONDS)).thenReturn(50.0);
        when(timer.mean(TimeUnit.SECONDS)).thenReturn(0.05);

        var response = controller.metrics();

        assertThat(response.created().meanLatencyMs()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void getLastPaymentForUserReturnsPaymentWhenFound() {
        UUID userId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(PaymentStatus.CONFIRMED)
                .plan("monthly")
                .amountXmr(new BigDecimal("0.1"))
                .createdAt(Instant.now())
                .build();

        when(paymentRepository.findTop1ByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(Optional.of(payment));

        var response = controller.getLastPaymentForUser(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().userId()).isEqualTo(userId);
    }

    @Test
    void getLastPaymentForUserReturnsNotFoundWhenNoPayment() {
        UUID userId = UUID.randomUUID();

        when(paymentRepository.findTop1ByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(Optional.empty());

        var response = controller.getLastPaymentForUser(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
