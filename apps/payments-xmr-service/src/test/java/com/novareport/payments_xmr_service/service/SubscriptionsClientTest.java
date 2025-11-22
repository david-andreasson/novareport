package com.novareport.payments_xmr_service.service;

import com.novareport.payments_xmr_service.dto.ActivateSubscriptionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked","null"})
class SubscriptionsClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    void activateSubscriptionThrowsWhenBaseUrlInvalid() {
        SubscriptionsClient client = new SubscriptionsClient(restTemplate, "https://evil.com", "secret");

        assertThatThrownBy(() -> client.activateSubscription(UUID.randomUUID(), "monthly", 30))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid subscriptions base URL");
    }

    @Test
    void activateSubscriptionCallsRestTemplateAndSucceedsOn2xx() {
        SubscriptionsClient client = new SubscriptionsClient(restTemplate, "http://localhost:8081", "secret-key");

        when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        UUID userId = UUID.randomUUID();
        client.activateSubscription(userId, "monthly", 30);

        ArgumentCaptor<HttpEntity<ActivateSubscriptionRequest>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        verify(restTemplate).exchange(
                org.mockito.ArgumentMatchers.eq("http://localhost:8081/api/v1/internal/subscriptions/activate"),
                org.mockito.ArgumentMatchers.eq(HttpMethod.POST),
                entityCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(Void.class)
        );

        HttpHeaders headers = entityCaptor.getValue().getHeaders();
        ActivateSubscriptionRequest body = entityCaptor.getValue().getBody();

        // Header and body basics
        assert headers.getFirst("X-INTERNAL-KEY").equals("secret-key");
        assert body != null;
        assert body.userId().equals(userId);
        assert body.plan().equals("monthly");
        assert body.durationDays() == 30;
    }

    @Test
    void activateSubscriptionThrowsSubscriptionActivationExceptionOnNon2xx() {
        SubscriptionsClient client = new SubscriptionsClient(restTemplate, "http://localhost:8081", "secret-key");

        when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.activateSubscription(UUID.randomUUID(), "monthly", 30))
                .isInstanceOf(SubscriptionActivationException.class)
                .hasMessageContaining("Unexpected response status");
    }

    @Test
    void activateSubscriptionWrapsRestClientException() {
        SubscriptionsClient client = new SubscriptionsClient(restTemplate, "http://localhost:8081", "secret-key");

        when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
                .thenThrow(new RestClientException("boom"));

        assertThatThrownBy(() -> client.activateSubscription(UUID.randomUUID(), "monthly", 30))
                .isInstanceOf(SubscriptionActivationException.class)
                .hasMessageContaining("Failed to activate subscription");
    }
}
