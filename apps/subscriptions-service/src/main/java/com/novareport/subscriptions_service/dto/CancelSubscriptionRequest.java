package com.novareport.subscriptions_service.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CancelSubscriptionRequest(
    @NotNull UUID userId,
    String reason
) {
}
