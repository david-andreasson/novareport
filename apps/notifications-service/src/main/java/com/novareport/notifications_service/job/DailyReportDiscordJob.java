package com.novareport.notifications_service.job;

import com.novareport.notifications_service.domain.NotificationReport;
import com.novareport.notifications_service.service.DiscordReportService;
import com.novareport.notifications_service.service.NotificationReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class DailyReportDiscordJob {

    private static final Logger log = LoggerFactory.getLogger(DailyReportDiscordJob.class);

    private final NotificationReportService reportService;
    private final DiscordReportService discordReportService;
    private final ZoneId reportZone;

    public DailyReportDiscordJob(
        NotificationReportService reportService,
        DiscordReportService discordReportService,
        @Value("${notifications.report.zone:Europe/Stockholm}") String zoneId
    ) {
        this.reportService = reportService;
        this.discordReportService = discordReportService;
        this.reportZone = ZoneId.of(zoneId);
    }

    @Scheduled(cron = "${notifications.discord.cron:0 5 6 * * *}", zone = "${notifications.report.zone:Europe/Stockholm}")
    public void sendDailyReportToDiscord() {
        LocalDate today = LocalDate.now(reportZone);
        reportService.findByReportDate(today).ifPresentOrElse(
            report -> {
                if (report.getDiscordSentAt() != null) {
                    log.info("Discord report already sent for {}. Skipping.", today);
                    return;
                }

                log.info("Sending daily report for {} to Discord", today);
                boolean sent = discordReportService.sendDailyReport(report);

                if (sent) {
                    report.setDiscordSentAt(Instant.now());
                    NotificationReport saved = reportService.save(report);
                    log.info("Marked Discord report as sent for {} (id={})", today, saved.getId());
                } else {
                    log.warn("Failed to send Discord report for {}. Will retry on next run.", today);
                }
            },
            () -> log.info("No notification report found for {}. Skipping Discord job.", today)
        );
    }
}
