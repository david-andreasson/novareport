package com.novareport.notifications_service.job;

import com.novareport.notifications_service.domain.NotificationReport;
import com.novareport.notifications_service.service.DiscordReportService;
import com.novareport.notifications_service.service.NotificationReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyReportDiscordJobTest {

    @Mock
    private NotificationReportService reportService;

    @Mock
    private DiscordReportService discordReportService;

    private DailyReportDiscordJob job;

    @BeforeEach
    void setUp() {
        job = new DailyReportDiscordJob(reportService, discordReportService, "Europe/Stockholm");
    }

    @Test
    void sendDailyReportToDiscordSkipsWhenNoReport() {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Stockholm"));
        when(reportService.findByReportDate(today)).thenReturn(Optional.empty());

        job.sendDailyReportToDiscord();

        verify(reportService).findByReportDate(eq(today));
        verifyNoInteractions(discordReportService);
    }

    @Test
    void sendDailyReportToDiscordSkipsWhenAlreadySent() {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Stockholm"));
        NotificationReport report = new NotificationReport();
        report.setReportDate(today);
        report.setDiscordSentAt(Instant.now());
        when(reportService.findByReportDate(today)).thenReturn(Optional.of(report));

        job.sendDailyReportToDiscord();

        verify(reportService).findByReportDate(eq(today));
        verifyNoMoreInteractions(reportService);
        verifyNoInteractions(discordReportService);
    }

    @Test
    void sendDailyReportToDiscordSendsAndMarksAsSentOnSuccess() {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Stockholm"));
        NotificationReport report = new NotificationReport();
        report.setReportDate(today);
        when(reportService.findByReportDate(today)).thenReturn(Optional.of(report));
        when(discordReportService.sendDailyReport(report)).thenReturn(true);
        when(reportService.save(report)).thenAnswer(invocation -> invocation.getArgument(0));

        job.sendDailyReportToDiscord();

        verify(reportService).findByReportDate(eq(today));
        verify(discordReportService).sendDailyReport(report);
        verify(reportService).save(report);
        assertThat(report.getDiscordSentAt()).isNotNull();
    }

    @Test
    void sendDailyReportToDiscordDoesNotSaveWhenSendFails() {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Stockholm"));
        NotificationReport report = new NotificationReport();
        report.setReportDate(today);
        when(reportService.findByReportDate(today)).thenReturn(Optional.of(report));
        when(discordReportService.sendDailyReport(report)).thenReturn(false);

        job.sendDailyReportToDiscord();

        verify(reportService).findByReportDate(eq(today));
        verify(discordReportService).sendDailyReport(report);
        verify(reportService, never()).save(any(NotificationReport.class));
        assertThat(report.getDiscordSentAt()).isNull();
    }
}
