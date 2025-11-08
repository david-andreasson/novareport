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
    
    // Monero address format constants
    private static final String MONERO_ADDRESS_PREFIX = "4";

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;

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

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public void confirmPayment(UUID paymentId) {
        log.info("Confirming payment {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (!payment.isPending()) {
            log.warn("Payment {} is not pending, current status: {}", paymentId, payment.getStatus());
            throw new InvalidPaymentStateException("Payment is not in pending state");
        }

        payment.confirm();
        Payment confirmedPayment = paymentRepository.save(payment);

        log.info("Payment {} confirmed, publishing event for subscription activation", paymentId);

        // Publish event that will be handled after transaction commit
        // This ensures subscription activation happens outside the transaction boundary
        eventPublisher.publishPaymentConfirmed(confirmedPayment);
    }

    private String generateFakeMoneroAddress() {
        // Real Monero addresses are 95 characters starting with 4
        // Generate exactly 94 random hex characters for better simulation
        java.security.SecureRandom random = new java.security.SecureRandom();
        byte[] bytes = new byte[47]; // 47 bytes = 94 hex chars
        random.nextBytes(bytes);
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return MONERO_ADDRESS_PREFIX + hexString.toString();
    }

    private int calculateDurationDays(String plan) {
        if (plan == null) {
            throw new IllegalArgumentException("Plan cannot be null");
        }
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
