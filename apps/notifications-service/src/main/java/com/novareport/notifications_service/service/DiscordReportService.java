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
import java.util.List;
import java.util.Map;

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

        Map<String, Object> payload = buildEmbedPayload(report.getReportDate(), report.getSummary());

        try {
            discordClient.sendPayload(webhookUrl, payload)
                .timeout(Duration.ofSeconds(5))
                .block();
            return true;
        } catch (Exception ex) {
            log.warn("Error sending Discord report for {}", report.getReportDate(), ex);
            return false;
        }
    }

    private Map<String, Object> buildEmbedPayload(LocalDate date, String summary) {
        String title = "Nova Report – Daily report " + date;

        String effectiveSummary = summary != null ? summary : "";

        // Discord embed description max length is 4096 characters.
        int maxDescriptionLength = 4096;
        if (effectiveSummary.length() > maxDescriptionLength) {
            int cut = Math.max(0, maxDescriptionLength - 3);
            effectiveSummary = effectiveSummary.substring(0, cut) + "...";
        }

        Map<String, Object> embed = Map.of(
            "title", title,
            "description", effectiveSummary
        );

        return Map.of(
            "content", "",
            "embeds", List.of(embed)
        );
    }
}
