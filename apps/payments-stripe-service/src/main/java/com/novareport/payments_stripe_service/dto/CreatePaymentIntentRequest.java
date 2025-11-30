package com.novareport.payments_stripe_service.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePaymentIntentRequest(
        @NotBlank(message = "Plan is required")
        String plan
) {
}
