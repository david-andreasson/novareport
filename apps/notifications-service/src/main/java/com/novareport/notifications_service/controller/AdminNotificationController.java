package com.novareport.notifications_service.controller;

import com.novareport.notifications_service.domain.NotificationReport;
import com.novareport.notifications_service.job.DailyReportEmailJob;
import com.novareport.notifications_service.service.NotificationReportService;
import com.novareport.notifications_service.service.ReportEmailService;
import com.novareport.notifications_service.service.WelcomeEmailService;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/notifications/admin")
@Tag(name = "Notifications Admin", description = "Admin operations and metrics for notifications service")
public class AdminNotificationController {

    private final MeterRegistry meterRegistry;
    private final NotificationReportService reportService;
    private final WelcomeEmailService welcomeEmailService;
    private final ReportEmailService reportEmailService;
    private final DailyReportEmailJob dailyReportEmailJob;
    private final String discordInviteUrl;

    public AdminNotificationController(
        MeterRegistry meterRegistry,
        NotificationReportService reportService,
        WelcomeEmailService welcomeEmailService,
        ReportEmailService reportEmailService,
        DailyReportEmailJob dailyReportEmailJob,
        @Value("${notifications.discord.invite-url:}") String discordInviteUrl
    ) {
        this.meterRegistry = meterRegistry;
        this.reportService = reportService;
        this.welcomeEmailService = welcomeEmailService;
        this.reportEmailService = reportEmailService;
        this.dailyReportEmailJob = dailyReportEmailJob;
        this.discordInviteUrl = discordInviteUrl;
    }

    @GetMapping("/metrics")
    @Operation(summary = "Notifications metrics", description = "Returns metrics for welcome emails and daily report emails")
    public NotificationsAdminMetricsResponse metrics() {
        double welcomeSuccess = getCounter("nova_notifications_welcome_emails_total", "success");
        double welcomeError = getCounter("nova_notifications_welcome_emails_total", "error");

        double dailySuccess = getCounter("nova_notifications_daily_report_emails_total", "success");
        double dailyNoReport = getCounter("nova_notifications_daily_report_emails_total", "no_report");
        double dailyAlreadySent = getCounter("nova_notifications_daily_report_emails_total", "already_sent");
        double dailyNoSubscribers = getCounter("nova_notifications_daily_report_emails_total", "no_subscribers");
        double dailyNoActiveSubscribers = getCounter("nova_notifications_daily_report_emails_total", "no_active_subscribers");
        double dailyMissingApiKey = getCounter("nova_notifications_daily_report_emails_total", "missing_api_key");
        double dailyError = getCounter("nova_notifications_daily_report_emails_total", "error");
        double dailyRecipientsTotal = getCounter("nova_notifications_daily_report_recipients_total", null);

        WelcomeEmailMetrics welcome = new WelcomeEmailMetrics(welcomeSuccess, welcomeError);
        DailyReportEmailMetrics daily = new DailyReportEmailMetrics(
            dailySuccess,
            dailyNoReport,
            dailyAlreadySent,
            dailyNoSubscribers,
            dailyNoActiveSubscribers,
            dailyMissingApiKey,
            dailyError,
            dailyRecipientsTotal
        );

        Optional<NotificationReport> latest = reportService.findLatest();
        String latestReportDate = latest.map(r -> r.getReportDate().toString()).orElse(null);
        boolean latestReportEmailSent = latest.map(r -> r.getEmailSentAt() != null).orElse(false);

        return new NotificationsAdminMetricsResponse(welcome, daily, latestReportDate, latestReportEmailSent);
    }

    @PostMapping("/welcome-email/resend")
    @Operation(summary = "Resend welcome email", description = "Resends the welcome email to a specific address")
    public ResponseEntity<Void> resendWelcomeEmail(@Valid @RequestBody ResendWelcomeEmailRequest request) {
        welcomeEmailService.sendWelcomeEmail(request.email(), request.firstName());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/daily-report/test-send")
    @Operation(summary = "Send test daily report email", description = "Sends the latest or specified daily report to a single email address")
    public ResponseEntity<Void> sendTestDailyReport(@Valid @RequestBody DailyReportTestSendRequest request) {
        Optional<NotificationReport> reportOpt;
        if (request.reportDate() != null && !request.reportDate().isBlank()) {
            LocalDate date = LocalDate.parse(request.reportDate());
            reportOpt = reportService.findByReportDate(date);
        } else {
            reportOpt = reportService.findLatest();
        }

        if (reportOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        NotificationReport report = reportOpt.get();
        reportEmailService.sendDailyReport(request.email(), report);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/daily-report/run-now")
    @Operation(summary = "Run daily report email job now", description = "Triggers the scheduled daily report email job immediately")
    public ResponseEntity<Void> runDailyReportNow() {
        dailyReportEmailJob.sendDailyReportEmails();
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/discord/invite")
    @Operation(summary = "Get Discord invite info", description = "Returns configured Discord invite URL for support and operations")
    public DiscordAdminInfoResponse getDiscordInvite() {
        String url = (discordInviteUrl != null && !discordInviteUrl.isBlank()) ? discordInviteUrl : null;
        boolean configured = url != null;
        return new DiscordAdminInfoResponse(configured, url);
    }

    private double getCounter(String name, String outcome) {
        if (outcome == null) {
            var meters = meterRegistry.find(name).counters();
            return meters.stream().mapToDouble(c -> c.count()).sum();
        }
        var counter = meterRegistry.find(name).tag("outcome", outcome).counter();
        return counter != null ? counter.count() : 0.0;
    }

    public record ResendWelcomeEmailRequest(
        @Email @NotBlank String email,
        String firstName
    ) {
    }

    public record DailyReportTestSendRequest(
        @Email @NotBlank String email,
        String reportDate
    ) {
    }

    public record WelcomeEmailMetrics(
        double success,
        double error
    ) {
    }

    public record DailyReportEmailMetrics(
        double success,
        double noReport,
        double alreadySent,
        double noSubscribers,
        double noActiveSubscribers,
        double missingApiKey,
        double error,
        double recipientsTotal
    ) {
    }

    public record NotificationsAdminMetricsResponse(
        WelcomeEmailMetrics welcomeEmails,
        DailyReportEmailMetrics dailyEmails,
        String latestReportDate,
        boolean latestReportEmailSent
    ) {
    }

    public record DiscordAdminInfoResponse(
        boolean configured,
        String inviteUrl
    ) {
    }
}
