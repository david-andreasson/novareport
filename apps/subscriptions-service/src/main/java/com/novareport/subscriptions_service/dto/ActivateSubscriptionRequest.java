package com.novareport.subscriptions_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ActivateSubscriptionRequest(
    @NotNull UUID userId,
    @NotBlank String plan,
    @Min(1) int durationDays,
    String txId
) {
}
