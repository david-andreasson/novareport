package com.novareport.reporter_service.service;

import com.novareport.reporter_service.domain.DailyReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReporterCoordinatorTest {

    private RssIngestService rssIngestService;
    private NewsApiIngestService newsApiIngestService;
    private DailyReportService dailyReportService;
    private ReportNotificationPublisher notificationPublisher;

    private ReporterCoordinator coordinator;

    @BeforeEach
    void setUp() {
        rssIngestService = mock(RssIngestService.class);
        newsApiIngestService = mock(NewsApiIngestService.class);
        dailyReportService = mock(DailyReportService.class);
        notificationPublisher = mock(ReportNotificationPublisher.class);
        coordinator = new ReporterCoordinator(rssIngestService, newsApiIngestService, dailyReportService, notificationPublisher);
    }

    @Test
    void ingestNowAggregatesResultsFromRssAndNewsApi() {
        RssIngestService.IngestResult rssResult = new RssIngestService.IngestResult(10, 7);
        RssIngestService.IngestResult apiResult = new RssIngestService.IngestResult(5, 3);

        when(rssIngestService.ingest()).thenReturn(rssResult);
        when(newsApiIngestService.ingest()).thenReturn(apiResult);

        RssIngestService.IngestResult combined = coordinator.ingestNow();

        assertThat(combined.attempted()).isEqualTo(15);
        assertThat(combined.stored()).isEqualTo(10);
    }

    @Test
    void buildReportDelegatesToDailyReportServiceAndPublishesNotification() {
        LocalDate date = LocalDate.of(2024, 1, 1);
        DailyReport report = new DailyReport();

        when(dailyReportService.buildReport(date)).thenReturn(report);

        DailyReport result = coordinator.buildReport(date);

        assertThat(result).isSameAs(report);
        verify(notificationPublisher).publish(report);
        verify(dailyReportService).buildReport(date);
    }
}
