package com.novareport.payments_xmr_service.dto;

import com.novareport.payments_xmr_service.domain.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentStatusResponse(
        UUID paymentId,
        PaymentStatus status,
        Instant createdAt,
        Instant confirmedAt
) {
}
