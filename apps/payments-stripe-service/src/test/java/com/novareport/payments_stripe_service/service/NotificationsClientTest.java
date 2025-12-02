package com.novareport.payments_stripe_service.service;

import com.novareport.payments_stripe_service.domain.Payment;
import com.novareport.payments_stripe_service.domain.PaymentStatus;
import com.novareport.payments_stripe_service.dto.PaymentConfirmedEmailRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationsClientTest {

    @Mock
    private RestTemplate restTemplate;

    private NotificationsClient notificationsClient;

    private static final String BASE_URL = "http://localhost:8083";
    private static final String INTERNAL_API_KEY = "test-key";

    @BeforeEach
    void setUp() {
        notificationsClient = new NotificationsClient(restTemplate, BASE_URL, INTERNAL_API_KEY);
        MDC.clear();
    }

    @Test
    void sendPaymentConfirmedEmailSuccessfully() {
        Payment payment = createTestPayment();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        notificationsClient.sendPaymentConfirmedEmail(payment);

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), entityCaptor.capture(), eq(Void.class));

        HttpEntity<?> capturedEntity = entityCaptor.getValue();
        assertThat(capturedEntity.getHeaders().get("X-INTERNAL-KEY")).containsExactly(INTERNAL_API_KEY);
        assertThat(capturedEntity.getBody()).isInstanceOf(PaymentConfirmedEmailRequest.class);
    }

    @Test
    void sendPaymentConfirmedEmailWithCorrelationId() {
        Payment payment = createTestPayment();
        MDC.put("correlationId", "test-correlation-id");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        notificationsClient.sendPaymentConfirmedEmail(payment);

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), entityCaptor.capture(), eq(Void.class));

        HttpEntity<?> capturedEntity = entityCaptor.getValue();
        assertThat(capturedEntity.getHeaders().get("X-Correlation-ID")).containsExactly("test-correlation-id");
    }

    @Test
    void sendPaymentConfirmedEmailSkipsWhenPaymentIsNull() {
        notificationsClient.sendPaymentConfirmedEmail(null);

        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));
    }

    @Test
    void sendPaymentConfirmedEmailSkipsWhenApiKeyIsBlank() {
        NotificationsClient clientWithoutKey = new NotificationsClient(restTemplate, BASE_URL, "");
        Payment payment = createTestPayment();

        clientWithoutKey.sendPaymentConfirmedEmail(payment);

        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));
    }

    @Test
    void sendPaymentConfirmedEmailHandlesRestClientException() {
        Payment payment = createTestPayment();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RestClientException("Connection failed"));

        notificationsClient.sendPaymentConfirmedEmail(payment);

        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    void sendPaymentConfirmedEmailHandlesUnexpectedRuntimeException() {
        Payment payment = createTestPayment();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        notificationsClient.sendPaymentConfirmedEmail(payment);

        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    void sendPaymentConfirmedEmailHandlesNon2xxResponse() {
        Payment payment = createTestPayment();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        notificationsClient.sendPaymentConfirmedEmail(payment);

        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    void validateBaseUrlThrowsForNullUrl() {
        assertThatThrownBy(() -> new NotificationsClient(restTemplate, null, INTERNAL_API_KEY)
                .sendPaymentConfirmedEmail(createTestPayment()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void validateBaseUrlThrowsForBlankUrl() {
        NotificationsClient client = new NotificationsClient(restTemplate, "", INTERNAL_API_KEY);
        assertThatThrownBy(() -> client.sendPaymentConfirmedEmail(createTestPayment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Notifications base URL is not configured");
    }

    @Test
    void validateBaseUrlThrowsForInvalidUrl() {
        NotificationsClient client = new NotificationsClient(restTemplate, "http://evil.com", INTERNAL_API_KEY);
        assertThatThrownBy(() -> client.sendPaymentConfirmedEmail(createTestPayment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid notifications base URL");
    }

    @Test
    void validateBaseUrlAcceptsLocalhostUrl() {
        NotificationsClient client = new NotificationsClient(restTemplate, "http://localhost:8083", INTERNAL_API_KEY);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        client.sendPaymentConfirmedEmail(createTestPayment());

        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    void validateBaseUrlAcceptsHttpNotificationsServiceUrl() {
        NotificationsClient client = new NotificationsClient(restTemplate, "http://notifications-service:8080", INTERNAL_API_KEY);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        client.sendPaymentConfirmedEmail(createTestPayment());

        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    void validateBaseUrlAcceptsHttpsNotificationsServiceUrl() {
        NotificationsClient client = new NotificationsClient(restTemplate, "https://notifications-service:8080", INTERNAL_API_KEY);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        client.sendPaymentConfirmedEmail(createTestPayment());

        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));
    }

    private Payment createTestPayment() {
        return Payment.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .plan("monthly")
                .durationDays(30)
                .status(PaymentStatus.CONFIRMED)
                .createdAt(Instant.now())
                .build();
    }
}
