package com.novareport.payments_xmr_service.controller;

import com.novareport.payments_xmr_service.domain.Payment;
import com.novareport.payments_xmr_service.domain.PaymentRepository;
import com.novareport.payments_xmr_service.domain.PaymentStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/payments/admin")
@Tag(name = "Payments Admin", description = "Admin operations and metrics for payments service")
public class AdminPaymentController {

    private final PaymentRepository paymentRepository;
    private final MeterRegistry meterRegistry;

    public AdminPaymentController(PaymentRepository paymentRepository, MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/metrics")
    @Operation(summary = "Payments metrics", description = "Returns aggregated metrics for created and confirmed payments")
    public PaymentsAdminMetricsResponse metrics() {
        long createdSuccess = paymentRepository.count();
        long createdError = getCounterSum("nova_payments_created_total", "error");

        long confirmedSuccess = paymentRepository.countByStatus(PaymentStatus.CONFIRMED);
        long confirmedInvalidState = getCounterSum("nova_payments_confirmed_total", "invalid_state");
        long confirmedError = getCounterSum("nova_payments_confirmed_total", "error");

        double meanCreateLatencyMs = getTimerMeanMillis("nova_payments_create_latency_seconds");
        double meanConfirmLatencyMs = getTimerMeanMillis("nova_payments_confirm_latency_seconds");
        double meanTimeToConfirmSeconds = getTimerMeanSeconds("nova_payments_time_to_confirm_seconds");

        PaymentCreatedMetrics created = new PaymentCreatedMetrics(
            createdSuccess,
            createdError,
            meanCreateLatencyMs
        );
        PaymentConfirmedMetrics confirmed = new PaymentConfirmedMetrics(
            confirmedSuccess,
            confirmedInvalidState,
            confirmedError,
            meanConfirmLatencyMs,
            meanTimeToConfirmSeconds
        );

        return new PaymentsAdminMetricsResponse(created, confirmed);
    }

    @GetMapping("/users/{userId}/last-payment")
    @Operation(summary = "Get last payment for user", description = "Returns the latest payment for a specific user, if any")
    public ResponseEntity<AdminPaymentResponse> getLastPaymentForUser(@PathVariable("userId") UUID userId) {
        Optional<Payment> paymentOpt = paymentRepository.findTop1ByUserIdOrderByCreatedAtDesc(userId);
        return paymentOpt
            .map(this::toAdminPaymentResponse)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private long getCounterSum(String name, String outcome) {
        return meterRegistry
            .find(name)
            .counters()
            .stream()
            .filter(counter -> outcome == null || outcome.equals(counter.getId().getTag("outcome")))
            .mapToLong(counter -> (long) counter.count())
            .sum();
    }

    private double getTimerMeanMillis(String name) {
        var timers = meterRegistry.find(name).timers();
        if (timers.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        long nonEmptyCount = 0;
        for (Timer timer : timers) {
            if (timer.count() == 0) {
                continue;
            }
            total += timer.mean(TimeUnit.MILLISECONDS);
            nonEmptyCount++;
        }
        if (nonEmptyCount == 0) {
            return 0.0;
        }
        return total / nonEmptyCount;
    }

    private double getTimerMeanSeconds(String name) {
        var timers = meterRegistry.find(name).timers();
        if (timers.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        long nonEmptyCount = 0;
        for (Timer timer : timers) {
            if (timer.count() == 0) {
                continue;
            }
            total += timer.mean(TimeUnit.SECONDS);
            nonEmptyCount++;
        }
        if (nonEmptyCount == 0) {
            return 0.0;
        }
        return total / nonEmptyCount;
    }

    private AdminPaymentResponse toAdminPaymentResponse(Payment payment) {
        return new AdminPaymentResponse(
            payment.getId(),
            payment.getUserId(),
            payment.getStatus(),
            payment.getCreatedAt(),
            payment.getConfirmedAt(),
            payment.getPlan(),
            payment.getAmountXmr()
        );
    }

    public record PaymentCreatedMetrics(
        long success,
        long error,
        double meanLatencyMs
    ) {
    }

    public record PaymentConfirmedMetrics(
        long success,
        long invalidState,
        long error,
        double meanLatencyMs,
        double meanTimeToConfirmSeconds
    ) {
    }

    public record PaymentsAdminMetricsResponse(
        PaymentCreatedMetrics created,
        PaymentConfirmedMetrics confirmed
    ) {
    }

    public record AdminPaymentResponse(
        UUID id,
        UUID userId,
        PaymentStatus status,
        Instant createdAt,
        Instant confirmedAt,
        String plan,
        BigDecimal amountXmr
    ) {
    }
}
