package com.novareport.payments_xmr_service.service;

import com.novareport.payments_xmr_service.domain.Payment;
import com.novareport.payments_xmr_service.domain.PaymentRepository;
import com.novareport.payments_xmr_service.domain.PaymentStatus;
import com.novareport.payments_xmr_service.dto.CreatePaymentResponse;
import com.novareport.payments_xmr_service.dto.PaymentStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final int PAYMENT_EXPIRY_HOURS = 24;
    private static final int MONTHLY_DURATION_DAYS = 30;
    private static final int YEARLY_DURATION_DAYS = 365;

    private final PaymentRepository paymentRepository;
    private final SubscriptionsClient subscriptionsClient;

    @Transactional
    public CreatePaymentResponse createPayment(UUID userId, String plan, BigDecimal amountXmr) {
        log.info("Creating payment for user {} with plan {} and amount {}", userId, plan, amountXmr);

        // Validate plan early to fail fast before any database operations
        int durationDays = calculateDurationDays(plan);
        String paymentAddress = generateFakeMoneroAddress();

        Payment payment = Payment.builder()
                .userId(userId)
                .paymentAddress(paymentAddress)
                .amountXmr(amountXmr)
                .plan(plan)
                .durationDays(durationDays)
                .status(PaymentStatus.PENDING)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        log.info("Created payment {} with address {}", savedPayment.getId(), paymentAddress);

        // Calculate expiry time for response (24 hours from now)
        Instant expiresAt = Instant.now().plus(PAYMENT_EXPIRY_HOURS, ChronoUnit.HOURS);

        return new CreatePaymentResponse(
                savedPayment.getId(),
                paymentAddress,
                amountXmr,
                expiresAt
        );
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(UUID paymentId, UUID userId) {
        log.debug("Getting payment status for payment {} and user {}", paymentId, userId);

        Payment payment = paymentRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        return new PaymentStatusResponse(
                payment.getId(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getConfirmedAt()
        );
    }

    @Transactional
    public void confirmPayment(UUID paymentId) {
        log.info("Confirming payment {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (!payment.isPending()) {
            log.warn("Payment {} is not pending, current status: {}", paymentId, payment.getStatus());
            throw new InvalidPaymentStateException("Payment is not in pending state");
        }

        payment.confirm();
        paymentRepository.save(payment);

        log.info("Payment {} confirmed, activating subscription", paymentId);

        try {
            subscriptionsClient.activateSubscription(
                    payment.getUserId(),
                    payment.getPlan(),
                    payment.getDurationDays()
            );
            log.info("Successfully confirmed payment {} and activated subscription", paymentId);
        } catch (SubscriptionActivationException e) {
            log.error("Failed to activate subscription for payment {}, marking as failed", paymentId, e);
            payment.fail();
            paymentRepository.save(payment);
            // Do not re-throw to prevent transaction rollback - payment is now marked as FAILED
        }
    }

    private String generateFakeMoneroAddress() {
        // Real Monero addresses are 95 characters starting with 4
        // This generates a more realistic fake address for better simulation
        String prefix = "4";
        String randomPart = UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", "");
        return prefix + randomPart.substring(0, 94);
    }

    private int calculateDurationDays(String plan) {
        return switch (plan.toLowerCase()) {
            case "monthly" -> MONTHLY_DURATION_DAYS;
            case "yearly" -> YEARLY_DURATION_DAYS;
            default -> {
                log.error("Unknown plan: {}", plan);
                throw new IllegalArgumentException("Unknown plan: " + plan);
            }
        };
    }
}
