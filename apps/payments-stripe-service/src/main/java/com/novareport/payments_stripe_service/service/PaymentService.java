package com.novareport.payments_stripe_service.service;

import com.novareport.payments_stripe_service.domain.Payment;
import com.novareport.payments_stripe_service.domain.PaymentRepository;
import com.novareport.payments_stripe_service.domain.PaymentStatus;
import com.novareport.payments_stripe_service.dto.CreatePaymentIntentResponse;
import com.novareport.payments_stripe_service.dto.PaymentStatusResponse;
import com.novareport.payments_stripe_service.util.LogSanitizer;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final int MONTHLY_DURATION_DAYS = 30;
    private static final int YEARLY_DURATION_DAYS = 365;
    private static final long MONTHLY_AMOUNT_FIAT = 49_00L;
    private static final long YEARLY_AMOUNT_FIAT = 499_00L;
    private static final String CURRENCY_SEK = "SEK";

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Transactional
    public CreatePaymentIntentResponse createPaymentIntent(UUID userId, String plan) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "error";
        String planTag = plan != null ? plan : "unknown";
        try {
            String normalizedPlan = normalizePlan(plan);
            int durationDays = calculateDurationDays(normalizedPlan);
            long amountFiat = calculateAmountFiat(normalizedPlan);

            log.info("Creating Stripe payment for user {} with plan {} and amount {} {}",
                    LogSanitizer.sanitize(userId),
                    LogSanitizer.sanitize(normalizedPlan),
                    amountFiat,
                    CURRENCY_SEK);

            if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
                throw new IllegalStateException("Stripe secret key is not configured");
            }
            Stripe.apiKey = stripeSecretKey;

            PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                    .setAmount(amountFiat)
                    .setCurrency(CURRENCY_SEK.toLowerCase(Locale.ROOT))
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .putMetadata("userId", userId.toString())
                    .putMetadata("plan", normalizedPlan)
                    .putMetadata("durationDays", String.valueOf(durationDays));

            PaymentIntent pi = PaymentIntent.create(builder.build());

            log.info("Stripe PaymentIntent created. ID: {}, clientSecret present: {}",
                    LogSanitizer.sanitize(pi.getId()), pi.getClientSecret() != null);

            Payment payment = Payment.builder()
                    .userId(userId)
                    .stripePaymentIntentId(pi.getId())
                    .amountFiat(amountFiat)
                    .currencyFiat(CURRENCY_SEK)
                    .plan(normalizedPlan)
                    .durationDays(durationDays)
                    .status(PaymentStatus.PENDING)
                    .build();

            Payment saved = Objects.requireNonNull(paymentRepository.save(payment));

            outcome = "success";

            return new CreatePaymentIntentResponse(
                    saved.getId(),
                    pi.getClientSecret(),
                    amountFiat,
                    CURRENCY_SEK
            );
        } catch (StripeException e) {
            log.error("Stripe payment intent creation failed: {}", LogSanitizer.sanitize(e.getMessage()), e);
            throw new IllegalStateException("Failed to create Stripe PaymentIntent");
        } catch (RuntimeException ex) {
            outcome = "error";
            throw ex;
        } finally {
            meterRegistry.counter("nova_stripe_payments_created_total",
                    "outcome", outcome,
                    "plan", planTag
            ).increment();
            sample.stop(
                    Timer.builder("nova_stripe_payments_create_latency_seconds")
                            .tag("outcome", outcome)
                            .tag("plan", planTag)
                            .register(meterRegistry)
            );
        }
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(UUID paymentId, UUID userId) {
        log.info("Getting Stripe payment status for payment {} and user {}",
                LogSanitizer.sanitize(paymentId), LogSanitizer.sanitize(userId));

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
        try {
            log.info("Confirming Stripe payment {}", LogSanitizer.sanitize(paymentId));

            payment = paymentRepository.findByIdWithLock(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

            if (payment.getPlan() != null) {
                planTag = payment.getPlan();
            }

            if (!payment.isPending()) {
                log.warn("Payment {} is not pending, current status: {}",
                        LogSanitizer.sanitize(paymentId), LogSanitizer.sanitize(payment.getStatus()));
                outcome = "invalid_state";
                throw new InvalidPaymentStateException("Payment is not in pending state");
            }

            payment.confirm();
            paymentRepository.save(payment);

            log.info("Payment {} confirmed, publishing event for subscription activation",
                    LogSanitizer.sanitize(paymentId));

            eventPublisher.publishPaymentConfirmed(payment);
            outcome = "success";
        } catch (RuntimeException ex) {
            if (!"invalid_state".equals(outcome)) {
                outcome = "error";
            }
            throw ex;
        } finally {
            meterRegistry.counter("nova_stripe_payments_confirmed_total",
                    "outcome", outcome,
                    "plan", planTag
            ).increment();

            if ("success".equals(outcome)
                    && payment != null
                    && payment.getCreatedAt() != null
                    && payment.getConfirmedAt() != null) {
                Duration timeToConfirm = Duration.between(payment.getCreatedAt(), payment.getConfirmedAt());
                Timer.builder("nova_stripe_payments_time_to_confirm_seconds")
                        .tag("plan", planTag)
                        .register(meterRegistry)
                        .record(timeToConfirm);
            }

            sample.stop(
                    Timer.builder("nova_stripe_payments_confirm_latency_seconds")
                            .tag("outcome", outcome)
                            .tag("plan", planTag)
                            .register(meterRegistry)
            );
        }
    }

    @Transactional
    public void confirmPaymentByStripePaymentIntentId(String stripePaymentIntentId) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for Stripe PaymentIntent " + stripePaymentIntentId));
        confirmPayment(payment.getId());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void markPaymentFailedByStripePaymentIntentId(String stripePaymentIntentId) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for Stripe PaymentIntent " + stripePaymentIntentId));
        if (payment.isPending()) {
            payment.fail();
            paymentRepository.save(payment);
            log.info("Marked Stripe payment {} as failed (PaymentIntent {})",
                    LogSanitizer.sanitize(payment.getId()), LogSanitizer.sanitize(stripePaymentIntentId));
        }
    }

    private String normalizePlan(String plan) {
        if (plan == null) {
            throw new InvalidPlanException("Plan cannot be null");
        }
        return plan.toLowerCase(Locale.ROOT);
    }

    private int calculateDurationDays(String plan) {
        return switch (plan) {
            case "monthly" -> MONTHLY_DURATION_DAYS;
            case "yearly" -> YEARLY_DURATION_DAYS;
            default -> {
                log.error("Unknown plan: {}", LogSanitizer.sanitize(plan));
                throw new InvalidPlanException("Unknown plan: " + plan);
            }
        };
    }

    private long calculateAmountFiat(String plan) {
        return switch (plan) {
            case "monthly" -> MONTHLY_AMOUNT_FIAT;
            case "yearly" -> YEARLY_AMOUNT_FIAT;
            default -> {
                log.error("Unknown plan for amount: {}", LogSanitizer.sanitize(plan));
                throw new InvalidPlanException("Unknown plan: " + plan);
            }
        };
    }
}
