package com.novareport.notifications_service.service;

import com.novareport.notifications_service.client.DiscordClient;
import com.novareport.notifications_service.domain.NotificationReport;
import com.novareport.notifications_service.util.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
            log.info(
                "Discord notifications are disabled; skipping send for {}",
                LogSanitizer.sanitize(report.getReportDate())
            );
            return false;
        }
        if (!StringUtils.hasText(webhookUrl)) {
            log.warn(
                "Discord webhook URL is not configured; skipping send for {}",
                LogSanitizer.sanitize(report.getReportDate())
            );
            return false;
        }

        Map<String, Object> payload = buildEmbedPayload(report.getReportDate(), report.getSummary());

        try {
            discordClient.sendPayload(webhookUrl, payload)
                .timeout(Duration.ofSeconds(5))
                .block();
            return true;
        } catch (Exception ex) {
            log.warn(
                "Error sending Discord report for {}",
                LogSanitizer.sanitize(report.getReportDate()),
                ex
            );
            return false;
        }
    }

    private Map<String, Object> buildEmbedPayload(LocalDate date, String summary) {
        String full = summary != null ? summary.trim() : "";
        String mainTitle = "Nova Report – 4 Hour Report " + date;

        List<Map<String, Object>> embeds = new ArrayList<>();

        List<Section> sections = splitIntoSections(full);

        int maxDescriptionLength = 4096; // Discord embed description limit

        if (sections.isEmpty()) {
            // Fallback: single embed with truncated full text
            String desc = truncate(stripMarkdownHeadings(full), maxDescriptionLength);
            Map<String, Object> embed = Map.of(
                "title", mainTitle,
                "description", desc
            );
            embeds.add(embed);
        } else {
            for (int i = 0; i < sections.size() && i < 4; i++) {
                Section section = sections.get(i);
                String desc = truncate(stripMarkdownHeadings(section.body()), maxDescriptionLength);
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

        // Scan the text line by line and only treat lines that are real headings
        // (e.g. "## Market Trends") as section boundaries. This avoids matching
        // words like "Outlook" when they appear inside normal paragraphs.
        String[] lines = full.split("\n");
        int offset = 0; // character index of the current line start in `full`

        for (String line : lines) {
            String trimmed = line.stripLeading();

            for (String heading : titles) {
                if (isSectionHeadingLine(trimmed, heading)) {
                    positions.add(new HeadingPos(heading, offset));
                    break;
                }
            }

            // +1 to account for the newline that was removed by split("\n")
            offset += line.length() + 1;
        }

        if (positions.isEmpty()) {
            return sections;
        }

        positions.sort((a, b) -> Integer.compare(a.index(), b.index()));

        for (int i = 0; i < positions.size(); i++) {
            HeadingPos current = positions.get(i);

            int headingIndex = current.index();

            // Find the end of the heading line (newline after the heading)
            int headingLineEnd = full.indexOf('\n', headingIndex);
            if (headingLineEnd < 0) {
                headingLineEnd = full.length();
            } else {
                headingLineEnd += 1; // move past the newline
            }

            // Body starts after the heading line
            int bodyStart = headingLineEnd;

            // Section ends at the start of the next heading line (its line start)
            int end = (i + 1 < positions.size()) ? positions.get(i + 1).index() : full.length();

            if (bodyStart >= 0 && bodyStart < end && end <= full.length()) {
                String part = full.substring(bodyStart, end).trim();
                if (!part.isEmpty()) {
                    sections.add(new Section(current.title(), part));
                }
            }
        }

        return sections;
    }

    private boolean isSectionHeadingLine(String trimmedLine, String heading) {
        if (trimmedLine == null || trimmedLine.isEmpty()) {
            return false;
        }

        String normalized = trimmedLine;
        if (normalized.startsWith("#")) {
            // Remove leading '#' markers and any following whitespace
            normalized = normalized.replaceFirst("^#+\\s*", "");
        }
        // Remove leading numbering like "1. ", "2) " etc
        normalized = normalized.replaceFirst("^[0-9]+[.)]\\s*", "");

        // Remove trailing colon or dashes ("Market Trends:", "Market Trends -")
        normalized = normalized.replaceFirst("[:\\-–]+\\s*$", "");

        String normalizedUpper = normalized.toUpperCase(Locale.ROOT);
        String headingUpper = heading.toUpperCase(Locale.ROOT);
        return normalizedUpper.equals(headingUpper);
    }

    private String stripMarkdownHeadings(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();

        for (String line : lines) {
            String trimmedLeading = line.stripLeading();

            if (trimmedLeading.startsWith("#")) {
                // Remove leading '#' characters and following single space
                String headingText = trimmedLeading.replaceFirst("^#+\\s*", "").trim();
                // Remove leading numeric prefixes like "1. ", "2) " to avoid clutter in headings
                headingText = headingText.replaceFirst("^[0-9]+[.)]\\s*", "").trim();
                if (!headingText.isEmpty()) {
                    sb.append("**").append(headingText).append("**");
                }
            } else {
                // Preserve original non-heading lines (minus trailing spaces) to keep line breaks
                sb.append(line.stripTrailing());
            }

            sb.append('\n');
        }

        return sb.toString().trim();
    }

    private record HeadingPos(String title, int index) { }

    private record Section(String title, String body) { }
}
