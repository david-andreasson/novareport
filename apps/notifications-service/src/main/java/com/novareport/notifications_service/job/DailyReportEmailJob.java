package com.novareport.notifications_service.job;

import com.novareport.notifications_service.client.AccountsClient;
import com.novareport.notifications_service.client.SubscriptionsClient;
import com.novareport.notifications_service.domain.NotificationReport;
import com.novareport.notifications_service.dto.ReportEmailSubscriberResponse;
import com.novareport.notifications_service.service.NotificationReportService;
import com.novareport.notifications_service.service.ReportEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class DailyReportEmailJob {

    private static final Logger log = LoggerFactory.getLogger(DailyReportEmailJob.class);

    private final NotificationReportService reportService;
    private final AccountsClient accountsClient;
    private final SubscriptionsClient subscriptionsClient;
    private final ReportEmailService reportEmailService;
    private final String accountsBaseUrl;
    private final String subscriptionsBaseUrl;
    private final String internalApiKey;
    private final ZoneId reportZone;

    public DailyReportEmailJob(
        NotificationReportService reportService,
        AccountsClient accountsClient,
        SubscriptionsClient subscriptionsClient,
        ReportEmailService reportEmailService,
        @Value("${accounts.base-url:http://accounts-service:8080}") String accountsBaseUrl,
        @Value("${subs.base-url:http://subscriptions-service:8080}") String subscriptionsBaseUrl,
        @Value("${internal.api-key:}") String internalApiKey,
        @Value("${notifications.report.zone:Europe/Stockholm}") String zoneId
    ) {
        this.reportService = reportService;
        this.accountsClient = accountsClient;
        this.subscriptionsClient = subscriptionsClient;
        this.reportEmailService = reportEmailService;
        this.accountsBaseUrl = accountsBaseUrl;
        this.subscriptionsBaseUrl = subscriptionsBaseUrl;
        this.internalApiKey = internalApiKey;
        this.reportZone = ZoneId.of(zoneId);
    }

    @Scheduled(cron = "${notifications.email.cron:0 0 6 * * *}", zone = "${notifications.report.zone:Europe/Stockholm}")
    public void sendDailyReportEmails() {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            log.warn("Internal API key is not configured; skipping daily report email job");
            return;
        }

        LocalDate today = LocalDate.now(reportZone);
        reportService.findByReportDate(today).ifPresentOrElse(
            report -> {
                if (report.getEmailSentAt() != null) {
                    log.info("Daily report email already sent for {}. Skipping.", today);
                    return;
                }

                List<ReportEmailSubscriberResponse> subscribers =
                    accountsClient.getReportEmailSubscribers(accountsBaseUrl, internalApiKey);
                if (subscribers.isEmpty()) {
                    log.info("No report email subscribers found; skipping email sending for {}", today);
                    return;
                }

                List<UUID> activeUserIds =
                    subscriptionsClient.getActiveUserIds(subscriptionsBaseUrl, internalApiKey);
                Set<UUID> activeUserIdSet = new HashSet<>(activeUserIds);

                long sentCount = subscribers.stream()
                    .filter(sub -> activeUserIdSet.contains(sub.userId()))
                    .peek(sub -> reportEmailService.sendDailyReport(sub.email(), report))
                    .count();

                if (sentCount > 0) {
                    report.setEmailSentAt(Instant.now());
                    reportService.save(report);
                    log.info("Sent daily report email for {} to {} recipients", today, sentCount);
                } else {
                    log.info("No active subscribers to send daily report email for {}", today);
                }
            },
            () -> log.info("No notification report found for {}. Skipping daily report email job.", today)
        );
    }
}
