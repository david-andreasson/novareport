package com.novareport.notifications_service.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class WelcomeEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private WelcomeEmailService service;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new WelcomeEmailService(mailSender, "from@test.local", meterRegistry);
    }

    @Test
    void sendWelcomeEmailBuildsAndSendsEmail() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        service.sendWelcomeEmail("user@example.com", "Anna");

        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();

        assertThat(message.getFrom()).isEqualTo("from@test.local");
        assertThat(message.getTo()).containsExactly("user@example.com");
        assertThat(message.getSubject()).contains("Välkommen till Nova Report");
        assertThat(message.getText()).contains("Hej Anna,");
        assertThat(message.getText()).contains("dagliga rapporter via e-post");
    }
}
