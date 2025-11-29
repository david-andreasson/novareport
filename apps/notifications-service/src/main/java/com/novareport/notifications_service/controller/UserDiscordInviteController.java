package com.novareport.notifications_service.controller;

import com.novareport.notifications_service.service.DiscordInviteService;
import com.novareport.notifications_service.service.SubscriptionAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications User", description = "User-facing notifications operations")
public class UserDiscordInviteController {

    private final SubscriptionAccessService subscriptionAccessService;
    private final DiscordInviteService discordInviteService;

    public UserDiscordInviteController(
        SubscriptionAccessService subscriptionAccessService,
        DiscordInviteService discordInviteService
    ) {
        this.subscriptionAccessService = subscriptionAccessService;
        this.discordInviteService = discordInviteService;
    }

    @PostMapping("/discord/invite/me")
    @Operation(
        summary = "Send Discord invite to current user",
        description = "Requires active subscription; sends invite link to the user's email address."
    )
    public ResponseEntity<Void> sendDiscordInviteForCurrentUser(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        boolean hasAccess = subscriptionAccessService.hasAccess(authorizationHeader);
        if (!hasAccess) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Subscription required for Discord access");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication");
        }

        String email = auth.getName();
        try {
            discordInviteService.sendInvite(email);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        }

        return ResponseEntity.accepted().build();
    }
}
