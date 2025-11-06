package com.novareport.payments_xmr_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreatePaymentResponse(
        UUID paymentId,
        String paymentAddress,
        BigDecimal amountXmr,
        Instant expiresAt
) {
}
