package com.novareport.notifications_service.service;

import com.novareport.notifications_service.util.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DiscordInviteService {

    private static final Logger log = LoggerFactory.getLogger(DiscordInviteService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String inviteUrl;

    public DiscordInviteService(
        JavaMailSender mailSender,
        @Value("${notifications.mail.from:noreply@novareport.local}") String fromAddress,
        @Value("${notifications.discord.invite-url:}") String inviteUrl
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.inviteUrl = inviteUrl;
    }

    public void sendInvite(String to) {
        String sanitizedTo = LogSanitizer.sanitize(to);
        if (!StringUtils.hasText(inviteUrl)) {
            log.warn("Discord invite URL is not configured; skipping send to {}", sanitizedTo);
            throw new IllegalStateException("Discord invite URL is not configured");
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject("Din Nova Report Discord-invite");
            message.setText(buildBody());
            mailSender.send(message);
            log.info("Sent Discord invite email to {}", sanitizedTo);
        } catch (Exception ex) {
            log.warn(
                "Failed to send Discord invite email to {}: {}",
                sanitizedTo,
                LogSanitizer.sanitize(ex.getMessage()),
                ex
            );
            throw new IllegalStateException("Failed to send Discord invite email");
        }
    }

    private String buildBody() {
        StringBuilder sb = new StringBuilder();
        sb.append("Hej,\n\n");
        sb.append("Tack för att du prenumererar på Nova Report.\n\n");
        sb.append("Här är din invite-länk till vår Discord-server:\n");
        sb.append(inviteUrl).append("\n\n");
        sb.append("Länken är personlig – dela den inte vidare.\n\n");
        sb.append("Vänliga hälsningar,\n");
        sb.append("Nova Report");
        return sb.toString();
    }
}
