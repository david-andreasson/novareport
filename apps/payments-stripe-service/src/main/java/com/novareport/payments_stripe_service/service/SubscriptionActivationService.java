package com.novareport.payments_stripe_service.service;

import com.novareport.payments_stripe_service.domain.Payment;
import com.novareport.payments_stripe_service.domain.PaymentRepository;
import com.novareport.payments_stripe_service.util.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionActivationService {

    private final SubscriptionsClient subscriptionsClient;
    private final PaymentRepository paymentRepository;
    private final NotificationsClient notificationsClient;

    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2),
        retryFor = {RestClientException.class, IOException.class},
        noRetryFor = {HttpClientErrorException.class}
    )
    public void activateSubscriptionForPayment(Payment payment) {
        try {
            subscriptionsClient.activateSubscription(
                    payment.getUserId(),
                    payment.getPlan(),
                    payment.getDurationDays()
            );
            log.info("Successfully activated subscription for payment {}",
                    LogSanitizer.sanitize(payment.getId()));
            notificationsClient.sendPaymentConfirmedEmail(payment);
        } catch (SubscriptionActivationException e) {
            log.error("Failed to activate subscription for payment {}, marking as failed",
                    LogSanitizer.sanitize(payment.getId()), e);
            markPaymentAsFailed(payment.getId());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPaymentAsFailed(UUID paymentId) {
        Payment payment = paymentRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
        payment.fail();
        paymentRepository.save(payment);
        log.info("Marked payment {} as failed", LogSanitizer.sanitize(paymentId));
    }
}
