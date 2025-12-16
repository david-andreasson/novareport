package com.novareport.payments_xmr_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotBlank(message = "Plan is required")
        String plan,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.0001", message = "Amount must be at least 0.0001 XMR")
        BigDecimal amountXmr
) {
}
