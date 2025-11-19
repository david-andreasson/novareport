package com.novareport.notifications_service.service;

import com.novareport.notifications_service.client.DiscordClient;
import com.novareport.notifications_service.domain.NotificationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;

@Service
public class DiscordReportService {

    private static final Logger log = LoggerFactory.getLogger(DiscordReportService.class);

    private final DiscordClient discordClient;
    private final boolean enabled;
    private final String webhookUrl;

    public DiscordReportService(
        DiscordClient discordClient,
        @Value("${notifications.discord.enabled:false}") boolean enabled,
        @Value("${notifications.discord.webhook-url:}") String webhookUrl
    ) {
        this.discordClient = discordClient;
        this.enabled = enabled;
        this.webhookUrl = webhookUrl;
    }

    public boolean sendDailyReport(NotificationReport report) {
        if (!enabled) {
            log.info("Discord notifications are disabled; skipping send for {}", report.getReportDate());
            return false;
        }
        if (!StringUtils.hasText(webhookUrl)) {
            log.warn("Discord webhook URL is not configured; skipping send for {}", report.getReportDate());
            return false;
        }

        String message = buildMessage(report.getReportDate(), report.getSummary());

        try {
            discordClient.sendMessage(webhookUrl, message)
                .timeout(Duration.ofSeconds(5))
                .block();
            return true;
        } catch (Exception ex) {
            log.warn("Error sending Discord report for {}", report.getReportDate(), ex);
            return false;
        }
    }

    private String buildMessage(LocalDate date, String summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("Nova Report – Daily report ")
            .append(date)
            .append("\n\n")
            .append(summary);
        return sb.toString();
    }
}
