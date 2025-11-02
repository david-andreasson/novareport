package com.novareport.subscriptions_service.dto;

import com.novareport.subscriptions_service.domain.Subscription;
import com.novareport.subscriptions_service.domain.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
    UUID userId,
    String plan,
    SubscriptionStatus status,
    Instant startAt,
    Instant endAt
) {
    public static SubscriptionResponse fromEntity(Subscription subscription) {
        return new SubscriptionResponse(
            subscription.getUserId(),
            subscription.getPlan(),
            subscription.getStatus(),
            subscription.getStartAt(),
            subscription.getEndAt()
        );
    }
}
