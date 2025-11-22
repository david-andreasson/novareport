package com.novareport.payments_xmr_service.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTest {

    @Test
    void isPendingReturnsTrueWhenStatusIsPending() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .paymentAddress("address")
                .amountXmr(BigDecimal.valueOf(1.23))
                .plan("monthly")
                .durationDays(30)
                .createdAt(Instant.now())
                .status(PaymentStatus.PENDING)
                .build();

        assertThat(payment.isPending()).isTrue();
    }

    @Test
    void isPendingReturnsFalseWhenStatusIsNotPending() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .paymentAddress("address")
                .amountXmr(BigDecimal.valueOf(1.23))
                .plan("monthly")
                .durationDays(30)
                .createdAt(Instant.now())
                .status(PaymentStatus.CONFIRMED)
                .build();

        assertThat(payment.isPending()).isFalse();
    }

    @Test
    void confirmUpdatesStatusAndConfirmedAt() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .paymentAddress("address")
                .amountXmr(BigDecimal.valueOf(1.23))
                .plan("monthly")
                .durationDays(30)
                .createdAt(Instant.now())
                .status(PaymentStatus.PENDING)
                .build();

        assertThat(payment.getConfirmedAt()).isNull();

        payment.confirm();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(payment.getConfirmedAt()).isNotNull();
    }

    @Test
    void failUpdatesStatusAndConfirmedAt() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .paymentAddress("address")
                .amountXmr(BigDecimal.valueOf(1.23))
                .plan("monthly")
                .durationDays(30)
                .createdAt(Instant.now())
                .status(PaymentStatus.PENDING)
                .build();

        assertThat(payment.getConfirmedAt()).isNull();

        payment.fail();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getConfirmedAt()).isNotNull();
    }
}
