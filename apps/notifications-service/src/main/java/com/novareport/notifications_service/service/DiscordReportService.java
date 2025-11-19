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
        String title = "Nova Report – Daily report " + date;
        String separator = "\n\n";

        // Discord max content length is 2000 characters.
        int maxLength = 2000;

        StringBuilder sb = new StringBuilder();
        sb.append(title);

        int remaining = maxLength - sb.length() - separator.length();
        if (remaining <= 0) {
            // Extremely unlikely, but guard just in case title is already too long.
            return sb.substring(0, Math.min(maxLength, sb.length()));
        }

        String effectiveSummary = summary != null ? summary : "";
        if (effectiveSummary.length() > remaining) {
            // Leave room for ellipsis when truncating.
            int cut = Math.max(0, remaining - 3);
            effectiveSummary = effectiveSummary.substring(0, cut) + "...";
        }

        sb.append(separator).append(effectiveSummary);
        if (sb.length() > maxLength) {
            return sb.substring(0, maxLength);
        }
        return sb.toString();
    }
}
