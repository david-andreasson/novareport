package com.novareport.notifications_service.service;

import com.novareport.notifications_service.client.AccountsClient;
import com.novareport.notifications_service.dto.UserContactResponse;
import com.novareport.notifications_service.util.LogSanitizer;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class PaymentConfirmationEmailService {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfirmationEmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final MeterRegistry meterRegistry;
    private final AccountsClient accountsClient;
    private final String accountsBaseUrl;
    private final String internalApiKey;
    private final String frontendBaseUrl;

    public PaymentConfirmationEmailService(
        JavaMailSender mailSender,
        @Value("${notifications.mail.from:noreply@novareport.local}") String fromAddress,
        MeterRegistry meterRegistry,
        AccountsClient accountsClient,
        @Value("${accounts.base-url:http://accounts-service:8080}") String accountsBaseUrl,
        @Value("${internal.api-key:}") String internalApiKey,
        @Value("${notifications.frontend.base-url:http://localhost:5173}") String frontendBaseUrl
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.meterRegistry = meterRegistry;
        this.accountsClient = accountsClient;
        this.accountsBaseUrl = accountsBaseUrl;
        this.internalApiKey = internalApiKey;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public void sendPaymentConfirmedEmail(UUID userId, String plan, int durationDays) {
        String outcome = "error";
        String planTag = plan != null ? plan : "unknown";
        String sanitizedUserId = LogSanitizer.sanitize(userId);

        if (!StringUtils.hasText(internalApiKey)) {
            log.warn("Internal API key is not configured; skipping payment confirmation email for user {}", sanitizedUserId);
            meterRegistry.counter("nova_notifications_payment_confirmation_emails_total", "outcome", "missing_api_key", "plan", planTag).increment();
            return;
        }

        try {
            UserContactResponse contact = accountsClient.getUserContact(accountsBaseUrl, internalApiKey, userId);
            if (contact == null || !StringUtils.hasText(contact.email())) {
                log.warn("No contact email found for user {}; skipping payment confirmation email", sanitizedUserId);
                meterRegistry.counter("nova_notifications_payment_confirmation_emails_total", "outcome", "no_email", "plan", planTag).increment();
                return;
            }

            String to = contact.email();
            String firstName = contact.firstName();

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject("Din Nova Report-prenumeration är nu aktiv");
            message.setText(buildBody(firstName, durationDays));

            mailSender.send(message);

            log.info("Sent payment confirmation email to {} for user {}", LogSanitizer.sanitize(to), sanitizedUserId);
            meterRegistry.counter("nova_notifications_payment_confirmation_emails_total", "outcome", "success", "plan", planTag).increment();
            outcome = "success";
        } catch (Exception ex) {
            log.warn(
                "Failed to send payment confirmation email for user {}: {}",
                sanitizedUserId,
                LogSanitizer.sanitize(ex.getMessage()),
                ex
            );
            meterRegistry.counter("nova_notifications_payment_confirmation_emails_total", "outcome", "error", "plan", planTag).increment();
        }
    }

    private String buildBody(String firstName, int durationDays) {
        String namePart = firstName != null && !firstName.isBlank() ? " " + firstName : "";
        StringBuilder sb = new StringBuilder();
        sb.append("Hej").append(namePart).append(",\n\n");
        sb.append("Tack för din betalning! Din Nova Report-prenumeration är nu aktiv i ")
            .append(durationDays)
            .append(" dagar.\n\n");

        sb.append("Dagliga rapporter via e-post:\n");
        sb.append("Om du vill få dagliga rapporter via e-post, gå till inställningssidan:\n");
        sb.append(frontendBaseUrl).append("/?view=settings\n");
        sb.append("Kryssa i \"Ta emot daglig rapport via e-post\" och spara.\n\n");

        sb.append("Discord-kanal:\n");
        sb.append("Som prenumerant får du tillgång till vår Discord-kanal där du kan diskutera marknaden och få snabb support.\n");
        sb.append("I webbtjänsten hittar du en knapp på din profilsida för att begära en personlig Discord-invite via e-post.\n\n");

        sb.append("Vänliga hälsningar,\n");
        sb.append("Nova Report");
        return sb.toString();
    }
}
