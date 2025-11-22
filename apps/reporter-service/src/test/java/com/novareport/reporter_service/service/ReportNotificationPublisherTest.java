package com.novareport.reporter_service.service;

import com.novareport.reporter_service.client.NotificationsClient;
import com.novareport.reporter_service.domain.DailyReport;
import com.novareport.reporter_service.domain.DailyReportRepository;
import com.novareport.reporter_service.dto.ReportReadyNotificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportNotificationPublisherTest {

    private NotificationsClient notificationsClient;
    private DailyReportRepository dailyReportRepository;

    private ReportNotificationPublisher publisher;

    @BeforeEach
    void setUp() {
        notificationsClient = mock(NotificationsClient.class);
        dailyReportRepository = mock(DailyReportRepository.class);
        publisher = new ReportNotificationPublisher(notificationsClient, dailyReportRepository, "internal-key", "http://notif");
    }

    @Test
    void publishByDateLogsWarningWhenNoReportFound() {
        LocalDate date = LocalDate.of(2024, 1, 1);
        when(dailyReportRepository.findByReportDate(date)).thenReturn(Optional.empty());

        publisher.publish(date);

        verify(notificationsClient, never()).notifyReportReady(any(), any(), any());
    }

    @Test
    void publishByDateDelegatesToPublishWhenReportExists() {
        LocalDate date = LocalDate.of(2024, 1, 2);
        DailyReport report = createReport(date);
        when(dailyReportRepository.findByReportDate(date)).thenReturn(Optional.of(report));
        when(notificationsClient.notifyReportReady(any(), any(), any())).thenReturn(Mono.empty());

        publisher.publish(date);

        verify(notificationsClient).notifyReportReady(any(), any(), any());
    }

    @Test
    void publishSkipsWhenNotificationsBaseUrlMissing() {
        publisher = new ReportNotificationPublisher(notificationsClient, dailyReportRepository, "internal-key", "");
        DailyReport report = createReport(LocalDate.now());

        publisher.publish(report);

        verify(notificationsClient, never()).notifyReportReady(any(), any(), any());
    }

    @Test
    void publishSkipsWhenInternalApiKeyMissing() {
        publisher = new ReportNotificationPublisher(notificationsClient, dailyReportRepository, "", "http://notif");
        DailyReport report = createReport(LocalDate.now());

        publisher.publish(report);

        verify(notificationsClient, never()).notifyReportReady(any(), any(), any());
    }

    @Test
    void publishCallsNotificationsClientWhenConfigured() {
        DailyReport report = createReport(LocalDate.of(2024, 1, 3));
        when(notificationsClient.notifyReportReady(eq("http://notif"), eq("internal-key"), any()))
            .thenReturn(Mono.empty());

        publisher.publish(report);

        ArgumentCaptor<ReportReadyNotificationRequest> captor = ArgumentCaptor.forClass(ReportReadyNotificationRequest.class);
        verify(notificationsClient).notifyReportReady(eq("http://notif"), eq("internal-key"), captor.capture());

        ReportReadyNotificationRequest payload = captor.getValue();
        assertThat(payload.reportId()).isEqualTo(report.getId());
        assertThat(payload.reportDate()).isEqualTo(report.getReportDate());
        assertThat(payload.summary()).isEqualTo(report.getSummary());
    }

    @Test
    void publishHandlesClientErrorViaOnErrorResume() {
        DailyReport report = createReport(LocalDate.of(2024, 1, 4));
        when(notificationsClient.notifyReportReady(any(), any(), any()))
            .thenReturn(Mono.error(new RuntimeException("fail")));

        assertThatCode(() -> publisher.publish(report))
            .doesNotThrowAnyException();
    }

    @Test
    void publishHandlesImmediateExceptionFromClient() {
        DailyReport report = createReport(LocalDate.of(2024, 1, 5));
        when(notificationsClient.notifyReportReady(any(), any(), any()))
            .thenThrow(new RuntimeException("boom"));

        assertThatCode(() -> publisher.publish(report))
            .doesNotThrowAnyException();
    }

    private DailyReport createReport(LocalDate date) {
        DailyReport report = new DailyReport();
        report.setId(UUID.randomUUID());
        report.setReportDate(date);
        report.setSummary("summary for " + date);
        report.setCreatedAt(Instant.now());
        return report;
    }
}
