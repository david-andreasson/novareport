package com.novareport.subscriptions_service.controller;

import com.novareport.subscriptions_service.domain.Subscription;
import com.novareport.subscriptions_service.dto.ActivateSubscriptionRequest;
import com.novareport.subscriptions_service.dto.ActiveSubscriptionUsersResponse;
import com.novareport.subscriptions_service.dto.CancelSubscriptionRequest;
import com.novareport.subscriptions_service.dto.SubscriptionResponse;
import com.novareport.subscriptions_service.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/subscriptions")
public class InternalSubscriptionController {

    private final SubscriptionService subscriptionService;

    public InternalSubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/activate")
    public ResponseEntity<SubscriptionResponse> activate(@Valid @RequestBody ActivateSubscriptionRequest request) {
        Subscription subscription = subscriptionService.activate(request.userId(), request.plan(), request.durationDays());
        return ResponseEntity.ok(SubscriptionResponse.fromEntity(subscription));
    }

    @PostMapping("/cancel")
    public ResponseEntity<SubscriptionResponse> cancel(@Valid @RequestBody CancelSubscriptionRequest request) {
        return subscriptionService.cancel(request.userId())
            .map(SubscriptionResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/active-users")
    public ActiveSubscriptionUsersResponse activeUsers() {
        Instant now = Instant.now();
        List<UUID> userIds = subscriptionService.findActiveUserIds(now);
        return new ActiveSubscriptionUsersResponse(userIds);
    }
}
