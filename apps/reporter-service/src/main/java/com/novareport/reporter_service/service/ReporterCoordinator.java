package com.novareport.reporter_service.service;

import com.novareport.reporter_service.domain.DailyReport;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ReporterCoordinator {

    private final RssIngestService rssIngestService;
    private final NewsApiIngestService newsApiIngestService;
    private final DailyReportService dailyReportService;
    private final ReportNotificationPublisher notificationPublisher;

    public ReporterCoordinator(
        RssIngestService rssIngestService,
        NewsApiIngestService newsApiIngestService,
        DailyReportService dailyReportService,
        ReportNotificationPublisher notificationPublisher
    ) {
        this.rssIngestService = rssIngestService;
        this.newsApiIngestService = newsApiIngestService;
        this.dailyReportService = dailyReportService;
        this.notificationPublisher = notificationPublisher;
    }

    public RssIngestService.IngestResult ingestNow() {
        RssIngestService.IngestResult rssResult = rssIngestService.ingest();
        RssIngestService.IngestResult apiResult = newsApiIngestService.ingest();

        long attempted = rssResult.attempted() + apiResult.attempted();
        long stored = rssResult.stored() + apiResult.stored();

        return new RssIngestService.IngestResult(attempted, stored);
    }

    public DailyReport buildReport(LocalDate date) {
        DailyReport report = dailyReportService.buildReport(date);
        notificationPublisher.publish(report);
        return report;
    }

}
