package com.novareport.notifications_service.controller;

import com.novareport.notifications_service.domain.NotificationReport;
import com.novareport.notifications_service.job.DailyReportEmailJob;
import com.novareport.notifications_service.service.NotificationReportService;
import com.novareport.notifications_service.service.ReportEmailService;
import com.novareport.notifications_service.service.WelcomeEmailService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminNotificationControllerTest {

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private NotificationReportService reportService;

    @Mock
    private WelcomeEmailService welcomeEmailService;

    @Mock
    private ReportEmailService reportEmailService;

    @Mock
    private DailyReportEmailJob dailyReportEmailJob;

    @Mock
    private Search search;

    @Mock
    private Counter counter;

    private AdminNotificationController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminNotificationController(
                meterRegistry,
                reportService,
                welcomeEmailService,
                reportEmailService,
                dailyReportEmailJob,
                "https://discord.gg/test"
        );
    }

    @Test
    void metricsReturnsAggregatedData() {
        when(meterRegistry.find(anyString())).thenReturn(search);
        when(search.tag(anyString(), anyString())).thenReturn(search);
        when(search.counter()).thenReturn(counter);
        when(search.counters()).thenReturn(List.of());
        when(counter.count()).thenReturn(10.0);
        when(reportService.findLatest()).thenReturn(Optional.empty());

        var response = controller.metrics();

        assertThat(response).isNotNull();
        assertThat(response.welcomeEmails()).isNotNull();
        assertThat(response.dailyEmails()).isNotNull();
    }

    @Test
    void resendWelcomeEmailCallsService() {
        var request = new AdminNotificationController.ResendWelcomeEmailRequest("test@example.com", "John");

        var response = controller.resendWelcomeEmail(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(welcomeEmailService).sendWelcomeEmail("test@example.com", "John");
    }

    @Test
    void sendTestDailyReportWithLatestReport() {
        NotificationReport report = mock(NotificationReport.class);
        when(reportService.findLatest()).thenReturn(Optional.of(report));
        var request = new AdminNotificationController.DailyReportTestSendRequest("test@example.com", null);

        var response = controller.sendTestDailyReport(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(reportEmailService).sendDailyReport("test@example.com", report);
    }

    @Test
    void sendTestDailyReportWithSpecificDate() {
        NotificationReport report = mock(NotificationReport.class);
        LocalDate date = LocalDate.of(2025, 1, 1);
        when(reportService.findByReportDate(date)).thenReturn(Optional.of(report));
        var request = new AdminNotificationController.DailyReportTestSendRequest("test@example.com", "2025-01-01");

        var response = controller.sendTestDailyReport(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(reportEmailService).sendDailyReport("test@example.com", report);
    }

    @Test
    void sendTestDailyReportReturnsNotFoundWhenNoReport() {
        when(reportService.findLatest()).thenReturn(Optional.empty());
        var request = new AdminNotificationController.DailyReportTestSendRequest("test@example.com", null);

        var response = controller.sendTestDailyReport(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(reportEmailService, never()).sendDailyReport(anyString(), any());
    }

    @Test
    void runDailyReportNowTriggersJob() {
        var response = controller.runDailyReportNow();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(dailyReportEmailJob).sendDailyReportEmails();
    }

    @Test
    void getDiscordInviteReturnsConfiguredUrl() {
        var response = controller.getDiscordInvite();

        assertThat(response.configured()).isTrue();
        assertThat(response.inviteUrl()).isEqualTo("https://discord.gg/test");
    }

    @Test
    void getDiscordInviteWhenNotConfigured() {
        controller = new AdminNotificationController(
                meterRegistry,
                reportService,
                welcomeEmailService,
                reportEmailService,
                dailyReportEmailJob,
                ""
        );

        var response = controller.getDiscordInvite();

        assertThat(response.configured()).isFalse();
        assertThat(response.inviteUrl()).isNull();
    }
}
