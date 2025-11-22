package com.novareport.notifications_service.service;

import com.novareport.notifications_service.domain.NotificationReport;
import com.novareport.notifications_service.domain.NotificationReportRepository;
import com.novareport.notifications_service.dto.ReportReadyNotificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class NotificationReportServiceTest {

    @Mock
    private NotificationReportRepository repository;

    @Mock
    private DiscordReportService discordReportService;

    @InjectMocks
    private NotificationReportService service;

    private UUID reportId;
    private LocalDate date;

    @BeforeEach
    void setUp() {
        reportId = UUID.randomUUID();
        date = LocalDate.now();
    }

    @Test
    void upsertReportCreatesNewReportAndDoesNotSetDiscordSentWhenNotSent() {
        ReportReadyNotificationRequest request = new ReportReadyNotificationRequest(reportId, date, "summary");

        when(repository.findByReportId(reportId)).thenReturn(Optional.empty());
        when(repository.findByReportDate(date)).thenReturn(Optional.empty());
        when(repository.save(any(NotificationReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(discordReportService.sendDailyReport(any(NotificationReport.class))).thenReturn(false);

        NotificationReport result = service.upsertReport(request);

        assertThat(result.getReportId()).isEqualTo(reportId);
        assertThat(result.getReportDate()).isEqualTo(date);
        assertThat(result.getSummary()).isEqualTo("summary");
        assertThat(result.getDiscordSentAt()).isNull();

        verify(repository).findByReportId(reportId);
        verify(repository).findByReportDate(date);
        verify(repository).save(any(NotificationReport.class));
        verify(discordReportService).sendDailyReport(result);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void upsertReportUpdatesExistingReportAndSetsDiscordSentWhenSent() {
        ReportReadyNotificationRequest request = new ReportReadyNotificationRequest(reportId, date, "summary");
        NotificationReport existing = new NotificationReport();
        existing.setReportId(UUID.randomUUID());
        existing.setReportDate(date.minusDays(1));

        when(repository.findByReportId(reportId)).thenReturn(Optional.of(existing));
        when(repository.save(any(NotificationReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(discordReportService.sendDailyReport(any(NotificationReport.class))).thenReturn(true);

        NotificationReport result = service.upsertReport(request);

        assertThat(result.getReportId()).isEqualTo(reportId);
        assertThat(result.getReportDate()).isEqualTo(date);
        assertThat(result.getSummary()).isEqualTo("summary");
        assertThat(result.getDiscordSentAt()).isNotNull();

        ArgumentCaptor<NotificationReport> savedCaptor = ArgumentCaptor.forClass(NotificationReport.class);
        verify(repository, times(2)).save(savedCaptor.capture());
        NotificationReport lastSaved = savedCaptor.getAllValues().get(1);
        assertThat(lastSaved.getDiscordSentAt()).isNotNull();
    }

    @Test
    void findLatestDelegatesToRepository() {
        NotificationReport report = new NotificationReport();
        when(repository.findTop1ByOrderByReportDateDesc()).thenReturn(Optional.of(report));

        Optional<NotificationReport> result = service.findLatest();

        assertThat(result).contains(report);
        verify(repository).findTop1ByOrderByReportDateDesc();
    }

    @Test
    void findByReportDateDelegatesToRepository() {
        NotificationReport report = new NotificationReport();
        when(repository.findByReportDate(date)).thenReturn(Optional.of(report));

        Optional<NotificationReport> result = service.findByReportDate(date);

        assertThat(result).contains(report);
        verify(repository).findByReportDate(date);
    }

    @Test
    void saveDelegatesToRepository() {
        NotificationReport report = new NotificationReport();
        when(repository.save(report)).thenReturn(report);

        NotificationReport result = service.save(report);

        assertThat(result).isSameAs(report);
        verify(repository).save(report);
    }

    @Test
    void saveRejectsNullReport() {
        assertThatThrownBy(() -> service.save(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("report must not be null");
    }
}
