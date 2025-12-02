package com.novareport.notifications_service.controller;

import com.novareport.notifications_service.service.DiscordInviteService;
import com.novareport.notifications_service.service.SubscriptionAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDiscordInviteControllerTest {

    @Mock
    private SubscriptionAccessService subscriptionAccessService;

    @Mock
    private DiscordInviteService discordInviteService;

    private UserDiscordInviteController controller;

    @BeforeEach
    void setUp() {
        controller = new UserDiscordInviteController(subscriptionAccessService, discordInviteService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void sendDiscordInviteSuccessfully() {
        when(subscriptionAccessService.hasAccess(anyString())).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@example.com", null)
        );

        var response = controller.sendDiscordInviteForCurrentUser("Bearer token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(discordInviteService).sendInvite("user@example.com");
    }

    @Test
    void sendDiscordInviteThrowsForbiddenWhenNoAccess() {
        when(subscriptionAccessService.hasAccess(anyString())).thenReturn(false);

        assertThatThrownBy(() -> controller.sendDiscordInviteForCurrentUser("Bearer token"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Subscription required");
    }

    @Test
    void sendDiscordInviteThrowsUnauthorizedWhenNoAuth() {
        when(subscriptionAccessService.hasAccess(anyString())).thenReturn(true);

        assertThatThrownBy(() -> controller.sendDiscordInviteForCurrentUser("Bearer token"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Missing authentication");
    }

    @Test
    void sendDiscordInviteThrowsServiceUnavailableOnError() {
        when(subscriptionAccessService.hasAccess(anyString())).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@example.com", null)
        );
        doThrow(new IllegalStateException("Discord not configured"))
                .when(discordInviteService).sendInvite(anyString());

        assertThatThrownBy(() -> controller.sendDiscordInviteForCurrentUser("Bearer token"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Discord not configured");
    }
}
