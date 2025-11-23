package com.novareport.subscriptions_service.controller;

import com.novareport.subscriptions_service.domain.Subscription;
import com.novareport.subscriptions_service.domain.SubscriptionStatus;
import com.novareport.subscriptions_service.dto.HasAccessResponse;
import com.novareport.subscriptions_service.dto.SubscriptionResponse;
import com.novareport.subscriptions_service.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private HttpServletRequest request;

    private SubscriptionController controller;

    @BeforeEach
    void setUp() {
        controller = new SubscriptionController(subscriptionService);
    }

    @Test
    void hasAccessReturnsResponseFromService() {
        UUID userId = UUID.randomUUID();
        when(request.getAttribute("uid")).thenReturn(userId.toString());
        when(subscriptionService.hasAccess(eq(userId), any(Instant.class))).thenReturn(true);

        ResponseEntity<HasAccessResponse> response = controller.hasAccess(request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().hasAccess()).isTrue();
        verify(subscriptionService).hasAccess(eq(userId), any(Instant.class));
    }

    @Test
    void hasAccessThrowsWhenUidMissing() {
        when(request.getAttribute("uid")).thenReturn(null);

        assertThatThrownBy(() -> controller.hasAccess(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .asString()
                .contains("400");
    }

    @Test
    void hasAccessThrowsWhenUidInvalid() {
        when(request.getAttribute("uid")).thenReturn("not-a-uuid");

        assertThatThrownBy(() -> controller.hasAccess(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .asString()
                .contains("400");
    }

    @Test
    void getActiveSubscriptionReturnsSubscriptionWhenPresent() {
        UUID userId = UUID.randomUUID();
        when(request.getAttribute("uid")).thenReturn(userId.toString());

        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setPlan("monthly");
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartAt(Instant.now());
        subscription.setEndAt(Instant.now().plusSeconds(3600));

        when(subscriptionService.findActiveSubscription(eq(userId), any(Instant.class)))
                .thenReturn(Optional.of(subscription));

        ResponseEntity<SubscriptionResponse> response = controller.getActiveSubscription(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        SubscriptionResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.userId()).isEqualTo(userId);
        assertThat(body.plan()).isEqualTo("monthly");
        assertThat(body.status()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void getActiveSubscriptionReturnsNotFoundWhenMissing() {
        UUID userId = UUID.randomUUID();
        when(request.getAttribute("uid")).thenReturn(userId.toString());
        when(subscriptionService.findActiveSubscription(eq(userId), any(Instant.class)))
                .thenReturn(Optional.empty());

        ResponseEntity<SubscriptionResponse> response = controller.getActiveSubscription(request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }
}
