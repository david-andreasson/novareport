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

        String paymentAddress = generateFakeMoneroAddress();
        int durationDays = calculateDurationDays(plan);
        Instant expiresAt = Instant.now().plus(PAYMENT_EXPIRY_HOURS, ChronoUnit.HOURS);

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
        } catch (SubscriptionActivationException e) {
            log.error("Failed to activate subscription for payment {}, marking as failed", paymentId);
            payment.fail();
            paymentRepository.save(payment);
            throw e;
        }

        log.info("Successfully confirmed payment {} and activated subscription", paymentId);
    }

    private String generateFakeMoneroAddress() {
        return "XMR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    private int calculateDurationDays(String plan) {
        return switch (plan.toLowerCase()) {
            case "monthly" -> MONTHLY_DURATION_DAYS;
            case "yearly" -> YEARLY_DURATION_DAYS;
            default -> {
                log.warn("Unknown plan: {}, defaulting to monthly", plan);
                yield MONTHLY_DURATION_DAYS;
            }
        };
    }
}
