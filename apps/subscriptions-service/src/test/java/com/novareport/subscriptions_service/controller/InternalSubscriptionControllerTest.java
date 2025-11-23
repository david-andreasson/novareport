package com.novareport.subscriptions_service.controller;

import com.novareport.subscriptions_service.domain.Subscription;
import com.novareport.subscriptions_service.domain.SubscriptionStatus;
import com.novareport.subscriptions_service.dto.ActivateSubscriptionRequest;
import com.novareport.subscriptions_service.dto.ActiveSubscriptionUsersResponse;
import com.novareport.subscriptions_service.dto.CancelSubscriptionRequest;
import com.novareport.subscriptions_service.dto.SubscriptionResponse;
import com.novareport.subscriptions_service.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalSubscriptionControllerTest {

    @Mock
    private SubscriptionService subscriptionService;

    private InternalSubscriptionController controller;

    @BeforeEach
    void setUp() {
        controller = new InternalSubscriptionController(subscriptionService);
    }

    @Test
    void activateReturnsSubscriptionResponse() {
        UUID userId = UUID.randomUUID();
        ActivateSubscriptionRequest request = new ActivateSubscriptionRequest(userId, "monthly", 30, "tx-1");

        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setPlan("monthly");
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartAt(Instant.now());
        subscription.setEndAt(Instant.now().plusSeconds(3600));

        when(subscriptionService.activate(userId, "monthly", 30)).thenReturn(subscription);

        ResponseEntity<SubscriptionResponse> response = controller.activate(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        SubscriptionResponse body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("SubscriptionResponse body must not be null");
        }
        assertThat(body.userId()).isEqualTo(userId);
        assertThat(body.plan()).isEqualTo("monthly");
        assertThat(body.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(subscriptionService).activate(userId, "monthly", 30);
    }

    @Test
    void cancelReturnsSubscriptionWhenPresent() {
        UUID userId = UUID.randomUUID();
        CancelSubscriptionRequest request = new CancelSubscriptionRequest(userId, "no longer needed");

        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setStatus(SubscriptionStatus.CANCELLED);

        when(subscriptionService.cancel(userId)).thenReturn(Optional.of(subscription));

        ResponseEntity<SubscriptionResponse> response = controller.cancel(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        SubscriptionResponse body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("SubscriptionResponse body must not be null");
        }
        assertThat(body.userId()).isEqualTo(userId);
    }

    @Test
    void cancelReturnsNotFoundWhenSubscriptionMissing() {
        UUID userId = UUID.randomUUID();
        CancelSubscriptionRequest request = new CancelSubscriptionRequest(userId, "no longer needed");

        when(subscriptionService.cancel(userId)).thenReturn(Optional.empty());

        ResponseEntity<SubscriptionResponse> response = controller.cancel(request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void activeUsersReturnsResponseWithUserIds() {
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(subscriptionService.findActiveUserIds(any())).thenReturn(ids);

        ActiveSubscriptionUsersResponse response = controller.activeUsers();

        assertThat(response.userIds()).isEqualTo(ids);
        verify(subscriptionService).findActiveUserIds(any());
    }
}
