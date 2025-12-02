package com.novareport.notifications_service.service;

import com.novareport.notifications_service.client.AccountsClient;
import com.novareport.notifications_service.dto.UserContactResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentConfirmationEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private Counter counter;

    private PaymentConfirmationEmailService service;

    private static final String FROM_ADDRESS = "noreply@test.com";
    private static final String ACCOUNTS_BASE_URL = "http://accounts:8080";
    private static final String INTERNAL_API_KEY = "test-key";
    private static final String FRONTEND_BASE_URL = "http://localhost:5173";

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(counter);
        service = new PaymentConfirmationEmailService(
                mailSender,
                FROM_ADDRESS,
                meterRegistry,
                accountsClient,
                ACCOUNTS_BASE_URL,
                INTERNAL_API_KEY,
                FRONTEND_BASE_URL
        );
    }

    @Test
    void sendPaymentConfirmedEmailSuccessfully() {
        UUID userId = UUID.randomUUID();
        UserContactResponse contact = new UserContactResponse(userId, "test@example.com", "John");
        when(accountsClient.getUserContact(ACCOUNTS_BASE_URL, INTERNAL_API_KEY, userId)).thenReturn(contact);

        service.sendPaymentConfirmedEmail(userId, "monthly", 30);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getTo()).containsExactly("test@example.com");
        assertThat(message.getFrom()).isEqualTo(FROM_ADDRESS);
        assertThat(message.getSubject()).contains("prenumeration");
        assertThat(message.getText()).contains("30 dagar");
        verify(counter).increment();
    }

    @Test
    void sendPaymentConfirmedEmailSkipsWhenNoApiKey() {
        service = new PaymentConfirmationEmailService(
                mailSender,
                FROM_ADDRESS,
                meterRegistry,
                accountsClient,
                ACCOUNTS_BASE_URL,
                "",
                FRONTEND_BASE_URL
        );
        UUID userId = UUID.randomUUID();

        service.sendPaymentConfirmedEmail(userId, "monthly", 30);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(counter).increment();
    }

    @Test
    void sendPaymentConfirmedEmailSkipsWhenNoEmail() {
        UUID userId = UUID.randomUUID();
        UserContactResponse contact = new UserContactResponse(userId, null, "John");
        when(accountsClient.getUserContact(ACCOUNTS_BASE_URL, INTERNAL_API_KEY, userId)).thenReturn(contact);

        service.sendPaymentConfirmedEmail(userId, "monthly", 30);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(counter).increment();
    }

    @Test
    void sendPaymentConfirmedEmailHandlesException() {
        UUID userId = UUID.randomUUID();
        when(accountsClient.getUserContact(ACCOUNTS_BASE_URL, INTERNAL_API_KEY, userId))
                .thenThrow(new RuntimeException("Connection failed"));

        service.sendPaymentConfirmedEmail(userId, "monthly", 30);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(counter).increment();
    }

    @Test
    void sendPaymentConfirmedEmailWithNullFirstName() {
        UUID userId = UUID.randomUUID();
        UserContactResponse contact = new UserContactResponse(userId, "test@example.com", null);
        when(accountsClient.getUserContact(ACCOUNTS_BASE_URL, INTERNAL_API_KEY, userId)).thenReturn(contact);

        service.sendPaymentConfirmedEmail(userId, "monthly", 30);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getText()).startsWith("Hej,");
    }
}
