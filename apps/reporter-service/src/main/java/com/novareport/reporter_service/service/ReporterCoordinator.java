package com.novareport.reporter_service.service;

import com.novareport.reporter_service.domain.DailyReport;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ReporterCoordinator {

    private final RssIngestService rssIngestService;
    private final DailyReportService dailyReportService;
    private final ReportNotificationPublisher notificationPublisher;

    public ReporterCoordinator(
        RssIngestService rssIngestService,
        DailyReportService dailyReportService,
        ReportNotificationPublisher notificationPublisher
    ) {
        this.rssIngestService = rssIngestService;
        this.dailyReportService = dailyReportService;
        this.notificationPublisher = notificationPublisher;
    }

    public RssIngestService.IngestResult ingestNow() {
        return rssIngestService.ingest();
    }

    public DailyReport buildReport(LocalDate date) {
        DailyReport report = dailyReportService.buildReport(date);
        notificationPublisher.publish(report);
        return report;
    }

}
