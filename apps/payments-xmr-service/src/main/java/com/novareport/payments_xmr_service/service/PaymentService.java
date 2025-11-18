package com.novareport.payments_xmr_service.service;

import com.novareport.payments_xmr_service.domain.Payment;
import com.novareport.payments_xmr_service.domain.PaymentRepository;
import com.novareport.payments_xmr_service.domain.PaymentStatus;
import com.novareport.payments_xmr_service.dto.CreatePaymentResponse;
import com.novareport.payments_xmr_service.dto.PaymentStatusResponse;
import com.novareport.payments_xmr_service.util.LogSanitizer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final int PAYMENT_EXPIRY_HOURS = 24;
    private static final int MONTHLY_DURATION_DAYS = 30;
    private static final int YEARLY_DURATION_DAYS = 365;
    
    // Monero address format constants
    private static final String MONERO_ADDRESS_PREFIX = "4";

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;
    private final MoneroWalletClient moneroWalletClient;
    private final MeterRegistry meterRegistry;

    @Value("${payments.fake-mode:true}")
    private boolean fakeMode;

    @Transactional
    public CreatePaymentResponse createPayment(UUID userId, String plan, BigDecimal amountXmr) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "error";
        String planTag = plan != null ? plan : "unknown";
        try {
            log.info("Creating payment for user {} with plan {} and amount {}", LogSanitizer.sanitize(userId), LogSanitizer.sanitize(plan), LogSanitizer.sanitize(amountXmr));

            int durationDays = calculateDurationDays(plan);

            String paymentAddress;
            Integer walletAccountIndex = null;
            Integer walletSubaddressIndex = null;

            if (fakeMode) {
                paymentAddress = generateFakeMoneroAddress();
            } else {
                int accountIndex = 0;
                MoneroWalletClient.MoneroSubaddress subaddress = moneroWalletClient.createSubaddress(accountIndex, "novareport-" + userId);
                paymentAddress = subaddress.address();
                walletAccountIndex = subaddress.accountIndex();
                walletSubaddressIndex = subaddress.subaddressIndex();
            }

            Payment payment = Payment.builder()
                    .userId(userId)
                    .paymentAddress(paymentAddress)
                    .amountXmr(amountXmr)
                    .plan(plan)
                    .durationDays(durationDays)
                    .walletAccountIndex(walletAccountIndex)
                    .walletSubaddressIndex(walletSubaddressIndex)
                    .status(PaymentStatus.PENDING)
                    .build();

            Payment savedPayment = paymentRepository.save(payment);
            assert savedPayment != null : "Saved payment should never be null";

            log.info("Created payment {} with address {}", savedPayment.getId(), paymentAddress);

            Instant expiresAt = Instant.now().plus(PAYMENT_EXPIRY_HOURS, ChronoUnit.HOURS);

            outcome = "success";
            return new CreatePaymentResponse(
                    savedPayment.getId(),
                    paymentAddress,
                    amountXmr,
                    expiresAt
            );
        } catch (RuntimeException ex) {
            outcome = "error";
            throw ex;
        } finally {
            meterRegistry.counter("nova_payments_created_total",
                    "outcome", outcome,
                    "plan", planTag
            ).increment();
            sample.stop(
                    Timer.builder("nova_payments_create_latency_seconds")
                            .tag("outcome", outcome)
                            .tag("plan", planTag)
                            .register(meterRegistry)
            );
        }
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(UUID paymentId, UUID userId) {
        log.info("Getting payment status for payment {} and user {}", LogSanitizer.sanitize(paymentId), LogSanitizer.sanitize(userId));

        Payment payment = paymentRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        return new PaymentStatusResponse(
                payment.getId(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getConfirmedAt()
        );
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void confirmPayment(UUID paymentId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "error";
        String planTag = "unknown";
        Payment payment = null;
        Payment savedPayment = null;
        try {
            log.info("Confirming payment {}", LogSanitizer.sanitize(paymentId));

            payment = paymentRepository.findByIdWithLock(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

            if (payment.getPlan() != null) {
                planTag = payment.getPlan();
            }

            if (!payment.isPending()) {
                log.warn("Payment {} is not pending, current status: {}", LogSanitizer.sanitize(paymentId), LogSanitizer.sanitize(payment.getStatus()));
                outcome = "invalid_state";
                throw new InvalidPaymentStateException("Payment is not in pending state");
            }

            payment.confirm();
            savedPayment = paymentRepository.save(payment);
            assert savedPayment != null : "Saved payment should never be null";

            log.info("Payment {} confirmed, publishing event for subscription activation", LogSanitizer.sanitize(paymentId));

            // Publish event that will be handled after transaction commit
            // This ensures subscription activation happens outside the transaction boundary
            eventPublisher.publishPaymentConfirmed(savedPayment);
            outcome = "success";
        } catch (RuntimeException ex) {
            if (!"invalid_state".equals(outcome)) {
                outcome = "error";
            }
            throw ex;
        } finally {
            meterRegistry.counter("nova_payments_confirmed_total",
                    "outcome", outcome,
                    "plan", planTag
            ).increment();

            if ("success".equals(outcome) && payment != null && payment.getCreatedAt() != null && payment.getConfirmedAt() != null) {
                Duration timeToConfirm = Duration.between(payment.getCreatedAt(), payment.getConfirmedAt());
                Timer.builder("nova_payments_time_to_confirm_seconds")
                        .tag("plan", planTag)
                        .register(meterRegistry)
                        .record(timeToConfirm);
            }

            sample.stop(
                    Timer.builder("nova_payments_confirm_latency_seconds")
                            .tag("outcome", outcome)
                            .tag("plan", planTag)
                            .register(meterRegistry)
            );
        }
    }

    private String generateFakeMoneroAddress() {
        // Real Monero addresses are 95 characters starting with 4
        // Generate exactly 94 random hex characters for better simulation
        // Using ThreadLocalRandom for better performance in simulated context
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder hexString = new StringBuilder(94); // Pre-allocate capacity
        for (int i = 0; i < 47; i++) { // 47 bytes = 94 hex chars
            hexString.append(String.format("%02x", random.nextInt(256)));
        }
        return MONERO_ADDRESS_PREFIX + hexString.toString();
    }

    private int calculateDurationDays(String plan) {
        if (plan == null) {
            throw new IllegalArgumentException("Plan cannot be null");
        }
        return switch (plan.toLowerCase(java.util.Locale.ROOT)) {
            case "monthly" -> MONTHLY_DURATION_DAYS;
            case "yearly" -> YEARLY_DURATION_DAYS;
            default -> {
                log.error("Unknown plan: {}", LogSanitizer.sanitize(plan));
                throw new InvalidPlanException("Unknown plan: " + plan);
            }
        };
    }
}
