package com.novareport.payments_stripe_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ActivateSubscriptionRequest(
        @NotNull(message = "User ID is required")
        UUID userId,

        @NotBlank(message = "Plan is required")
        String plan,

        @Min(value = 1, message = "Duration must be at least 1 day")
        int durationDays
) {
}
