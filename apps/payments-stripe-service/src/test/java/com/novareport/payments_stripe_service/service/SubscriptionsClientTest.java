package com.novareport.payments_stripe_service.service;

import com.novareport.payments_stripe_service.dto.ActivateSubscriptionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"null", "unchecked"})
class SubscriptionsClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    void activateSubscriptionCallsSubscriptionsServiceWithCorrectUrlAndBody() {
        String baseUrl = "http://subscriptions-service:8080";
        String internalKey = "internal-key";
        SubscriptionsClient client = new SubscriptionsClient(restTemplate, baseUrl, internalKey);

        UUID userId = UUID.randomUUID();

        when(restTemplate.exchange(
                any(String.class),
                any(HttpMethod.class),
                any(HttpEntity.class),
                any(Class.class)
        )).thenReturn(ResponseEntity.ok().build());

        client.activateSubscription(userId, "monthly", 30);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpMethod> methodCaptor = ArgumentCaptor.forClass(HttpMethod.class);
        ArgumentCaptor<HttpEntity<ActivateSubscriptionRequest>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        verify(restTemplate).exchange(urlCaptor.capture(), methodCaptor.capture(), entityCaptor.capture(), any(Class.class));

        String url = urlCaptor.getValue();
        HttpMethod method = methodCaptor.getValue();
        ActivateSubscriptionRequest body = entityCaptor.getValue().getBody();

        assert url.equals("http://subscriptions-service:8080/api/v1/internal/subscriptions/activate");
        assert method == HttpMethod.POST;
        assert body != null;
        assert body.userId().equals(userId);
        assert body.plan().equals("monthly");
        assert body.durationDays() == 30;
    }

    @Test
    void activateSubscriptionThrowsWhenBaseUrlInvalid() {
        String baseUrl = "https://example.com";
        String internalKey = "internal-key";
        SubscriptionsClient client = new SubscriptionsClient(restTemplate, baseUrl, internalKey);

        assertThatThrownBy(() -> client.activateSubscription(UUID.randomUUID(), "monthly", 30))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid subscriptions base URL");
    }
}
