package com.novareport.notifications_service.service;

import com.novareport.notifications_service.util.LogSanitizer;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class WelcomeEmailService {

    private static final Logger log = LoggerFactory.getLogger(WelcomeEmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final MeterRegistry meterRegistry;

    public WelcomeEmailService(JavaMailSender mailSender,
                               @Value("${notifications.mail.from:noreply@novareport.local}") String fromAddress,
                               MeterRegistry meterRegistry) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.meterRegistry = meterRegistry;
    }

    public void sendWelcomeEmail(String to, String firstName) {
        String sanitizedTo = LogSanitizer.sanitize(to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject("Välkommen till Nova Report");
            message.setText(buildBody(firstName));
            mailSender.send(message);
            log.info("Sent welcome email to {}", sanitizedTo);
            meterRegistry.counter("nova_notifications_welcome_emails_total", "outcome", "success").increment();
        } catch (Exception ex) {
            log.warn(
                "Failed to send welcome email to {}: {}",
                sanitizedTo,
                LogSanitizer.sanitize(ex.getMessage()),
                ex
            );
            meterRegistry.counter("nova_notifications_welcome_emails_total", "outcome", "error").increment();
        }
    }

    private String buildBody(String firstName) {
        String namePart = firstName != null && !firstName.isBlank() ? " " + firstName : "";
        StringBuilder sb = new StringBuilder();
        sb.append("Hej").append(namePart).append(",\n\n");
        sb.append("Välkommen till Nova Report!\n\n");
        sb.append("Du kan redan nu logga in och läsa den senaste rapporten direkt i webbtjänsten – helt gratis.\n\n");
        sb.append("Om du vill få dagliga rapporter via e-post och få tillgång till vår Discord-kanal\n");
        sb.append("behöver du ha en aktiv prenumeration.\n\n");
        sb.append("Vänliga hälsningar,\n");
        sb.append("Nova Report");
        return sb.toString();
    }
}
