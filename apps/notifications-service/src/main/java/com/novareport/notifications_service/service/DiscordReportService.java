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
import java.util.ArrayList;
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
        String full = summary != null ? summary.trim() : "";
        String mainTitle = "Nova Report – Daily report " + date;

        List<Map<String, Object>> embeds = new ArrayList<>();

        List<Section> sections = splitIntoSections(full);

        int maxDescriptionLength = 4096; // Discord embed description limit

        if (sections.isEmpty()) {
            // Fallback: single embed with truncated full text
            String desc = truncate(full, maxDescriptionLength);
            Map<String, Object> embed = Map.of(
                "title", mainTitle,
                "description", desc
            );
            embeds.add(embed);
        } else {
            for (int i = 0; i < sections.size() && i < 4; i++) {
                Section section = sections.get(i);
                String desc = truncate(section.body(), maxDescriptionLength);
                String embedTitle = i == 0 ? mainTitle : section.title();

                Map<String, Object> embed = Map.of(
                    "title", embedTitle,
                    "description", desc
                );
                embeds.add(embed);
            }
        }

        return Map.of(
            "content", "",
            "embeds", embeds
        );
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        int cut = Math.max(0, maxLength - 3);
        return text.substring(0, cut) + "...";
    }

    private List<Section> splitIntoSections(String full) {
        List<Section> sections = new ArrayList<>();
        if (full == null || full.isBlank()) {
            return sections;
        }

        String[] titles = {"Executive Summary", "Key Developments", "Market Trends", "Outlook"};
        List<HeadingPos> positions = new ArrayList<>();

        for (String heading : titles) {
            int idx = indexOfIgnoreCase(full, heading);
            if (idx >= 0) {
                positions.add(new HeadingPos(heading, idx));
            }
        }

        if (positions.isEmpty()) {
            return sections;
        }

        positions.sort((a, b) -> Integer.compare(a.index(), b.index()));

        for (int i = 0; i < positions.size(); i++) {
            HeadingPos current = positions.get(i);
            int start = (i == 0 && current.index() > 0) ? 0 : current.index();
            int end = (i + 1 < positions.size()) ? positions.get(i + 1).index() : full.length();

            if (start >= 0 && start < end && end <= full.length()) {
                String part = full.substring(start, end).trim();
                if (!part.isEmpty()) {
                    sections.add(new Section(current.title(), part));
                }
            }
        }

        return sections;
    }

    private int indexOfIgnoreCase(String text, String search) {
        return text.toLowerCase().indexOf(search.toLowerCase());
    }

    private record HeadingPos(String title, int index) { }

    private record Section(String title, String body) { }
}
