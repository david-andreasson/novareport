package com.novareport.subscriptions_service.controller;

import com.novareport.subscriptions_service.domain.SubscriptionStatus;
import com.novareport.subscriptions_service.domain.SubscriptionRepository;
import com.novareport.subscriptions_service.dto.SubscriptionResponse;
import com.novareport.subscriptions_service.service.SubscriptionService;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions/admin")
@Tag(name = "Subscriptions Admin", description = "Admin operations and metrics for subscriptions service")
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;
    private final MeterRegistry meterRegistry;

    public AdminSubscriptionController(
        SubscriptionService subscriptionService,
        SubscriptionRepository subscriptionRepository,
        MeterRegistry meterRegistry
    ) {
        this.subscriptionService = subscriptionService;
        this.subscriptionRepository = subscriptionRepository;
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/metrics")
    @Operation(summary = "Subscriptions metrics", description = "Returns metrics for active subscriptions and activation attempts")
    public AdminSubscriptionMetricsResponse metrics() {
        Instant now = Instant.now();
        long activeSubscriptions = subscriptionRepository.countByStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
            SubscriptionStatus.ACTIVE,
            now,
            now
        );

        long activatedSuccess = getActivationCount("success");
        long activatedError = getActivationCount("error");

        return new AdminSubscriptionMetricsResponse(activeSubscriptions, activatedSuccess, activatedError);
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get active subscription for user", description = "Returns the active subscription for a specific user, if any")
    public ResponseEntity<SubscriptionResponse> getActiveSubscriptionForUser(@PathVariable("userId") UUID userId) {
        return subscriptionService.findActiveSubscription(userId, Instant.now())
            .map(SubscriptionResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private long getActivationCount(String outcome) {
        return meterRegistry
            .find("nova_subscriptions_activated_total")
            .counters()
            .stream()
            .filter(counter -> outcome.equals(counter.getId().getTag("outcome")))
            .mapToLong(counter -> (long) counter.count())
            .sum();
    }

    public record AdminSubscriptionMetricsResponse(
        long activeSubscriptions,
        long activatedSuccess,
        long activatedError
    ) {
    }
}
