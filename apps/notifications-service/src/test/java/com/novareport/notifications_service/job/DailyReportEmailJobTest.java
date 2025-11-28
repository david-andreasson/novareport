package com.novareport.notifications_service.job;

import com.novareport.notifications_service.client.AccountsClient;
import com.novareport.notifications_service.client.SubscriptionsClient;
import com.novareport.notifications_service.domain.NotificationReport;
import com.novareport.notifications_service.dto.ReportEmailSubscriberResponse;
import com.novareport.notifications_service.service.NotificationReportService;
import com.novareport.notifications_service.service.ReportEmailService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyReportEmailJobTest {

    @Mock
    private NotificationReportService reportService;

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private SubscriptionsClient subscriptionsClient;

    @Mock
    private ReportEmailService reportEmailService;

    private MeterRegistry meterRegistry;

    private DailyReportEmailJob job;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        job = new DailyReportEmailJob(
                reportService,
                accountsClient,
                subscriptionsClient,
                reportEmailService,
                meterRegistry,
                "http://accounts",
                "http://subs",
                "key",
                "Europe/Stockholm"
        );
    }

    @Test
    void sendDailyReportEmailsSkipsWhenInternalApiKeyMissing() {
        DailyReportEmailJob localJob = new DailyReportEmailJob(
                reportService,
                accountsClient,
                subscriptionsClient,
                reportEmailService,
                meterRegistry,
                "http://accounts",
                "http://subs",
                " ",
                "Europe/Stockholm"
        );

        localJob.sendDailyReportEmails();

        verifyNoInteractions(reportService, accountsClient, subscriptionsClient, reportEmailService);
    }

    @Test
    void sendDailyReportEmailsSkipsWhenNoReportForToday() {
        LocalDate today = LocalDate.now();
        when(reportService.findByReportDate(today)).thenReturn(Optional.empty());

        job.sendDailyReportEmails();

        verify(reportService).findByReportDate(eq(today));
        verifyNoInteractions(accountsClient, subscriptionsClient, reportEmailService);
    }

    @Test
    void sendDailyReportEmailsSkipsWhenEmailAlreadySent() {
        LocalDate today = LocalDate.now();
        NotificationReport report = new NotificationReport();
        report.setReportDate(today);
        report.setEmailSentAt(Instant.now());
        when(reportService.findByReportDate(today)).thenReturn(Optional.of(report));

        job.sendDailyReportEmails();

        verify(reportService).findByReportDate(eq(today));
        verifyNoInteractions(accountsClient, subscriptionsClient, reportEmailService);
    }

    @Test
    void sendDailyReportEmailsSendsToActiveSubscribersAndMarksSent() {
        LocalDate today = LocalDate.now();
        NotificationReport report = new NotificationReport();
        report.setReportDate(today);
        when(reportService.findByReportDate(today)).thenReturn(Optional.of(report));

        UUID activeUser = UUID.randomUUID();
        UUID inactiveUser = UUID.randomUUID();

        List<ReportEmailSubscriberResponse> subscribers = List.of(
                new ReportEmailSubscriberResponse(activeUser, "active@example.com"),
                new ReportEmailSubscriberResponse(inactiveUser, "inactive@example.com")
        );
        when(accountsClient.getReportEmailSubscribers("http://accounts", "key"))
                .thenReturn(subscribers);

        when(subscriptionsClient.getActiveUserIds("http://subs", "key"))
                .thenReturn(List.of(activeUser));

        job.sendDailyReportEmails();

        verify(reportEmailService).sendDailyReport("active@example.com", report);
        verify(reportEmailService, never()).sendDailyReport("inactive@example.com", report);
        verify(reportService).save(report);
        verify(accountsClient).getReportEmailSubscribers("http://accounts", "key");
        verify(subscriptionsClient).getActiveUserIds("http://subs", "key");
    }
}
