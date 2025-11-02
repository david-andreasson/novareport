package com.novareport.subscriptions_service.controller;

import com.novareport.subscriptions_service.domain.Subscription;
import com.novareport.subscriptions_service.dto.HasAccessResponse;
import com.novareport.subscriptions_service.dto.SubscriptionResponse;
import com.novareport.subscriptions_service.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions/me")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/has-access")
    public ResponseEntity<HasAccessResponse> hasAccess(HttpServletRequest request) {
        UUID userId = resolveUserId(request);
        boolean hasAccess = subscriptionService.hasAccess(userId, Instant.now());
        return ResponseEntity.ok(new HasAccessResponse(hasAccess));
    }

    @GetMapping
    public ResponseEntity<SubscriptionResponse> getActiveSubscription(HttpServletRequest request) {
        UUID userId = resolveUserId(request);
        Optional<Subscription> subscription = subscriptionService.findActiveSubscription(userId, Instant.now());
        return subscription
            .map(SubscriptionResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private UUID resolveUserId(HttpServletRequest request) {
        Object uidAttr = request.getAttribute("uid");
        if (uidAttr instanceof String value) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid uid claim");
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing uid claim");
    }
}
