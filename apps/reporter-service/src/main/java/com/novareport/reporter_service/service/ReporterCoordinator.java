package com.novareport.reporter_service.service;

import com.novareport.reporter_service.client.NotificationsClient;
import com.novareport.reporter_service.domain.DailyReport;
import com.novareport.reporter_service.domain.DailyReportRepository;
import com.novareport.reporter_service.domain.NewsItem;
import com.novareport.reporter_service.domain.NewsItemRepository;
import com.novareport.reporter_service.dto.ReportReadyNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

@Service
public class ReporterCoordinator {

    private static final Logger log = LoggerFactory.getLogger(ReporterCoordinator.class);

    private final RssIngestService rssIngestService;
    private final DailyReportService dailyReportService;
    private final NewsItemRepository newsItemRepository;
    private final DailyReportRepository dailyReportRepository;
    private final NotificationsClient notificationsClient;
    private final String internalApiKey;
    private final String notificationsBaseUrl;

    public ReporterCoordinator(
        RssIngestService rssIngestService,
        DailyReportService dailyReportService,
        NewsItemRepository newsItemRepository,
        DailyReportRepository dailyReportRepository,
        NotificationsClient notificationsClient,
        @Value("${internal.api-key:}") String internalApiKey,
        @Value("${notif.base-url:}") String notificationsBaseUrl
    ) {
        this.rssIngestService = rssIngestService;
        this.dailyReportService = dailyReportService;
        this.newsItemRepository = newsItemRepository;
        this.dailyReportRepository = dailyReportRepository;
        this.notificationsClient = notificationsClient;
        this.internalApiKey = internalApiKey;
        this.notificationsBaseUrl = notificationsBaseUrl;
    }

    public RssIngestService.IngestResult ingestNow() {
        return rssIngestService.ingest();
    }

    public DailyReport buildReport(LocalDate date) {
        DailyReport report = dailyReportService.buildReport(date);
        notifyReportReady(date);
        return report;
    }

    public void notifyReportReady(LocalDate date) {
        if (!StringUtils.hasText(notificationsBaseUrl)) {
            log.info("Notification base URL not configured, skipping notify for {}", date);
            return;
        }
        if (!StringUtils.hasText(internalApiKey)) {
            log.warn("Internal API key missing, skipping notify for {}", date);
            return;
        }

        DailyReport report = dailyReportRepository.findByReportDate(date)
            .orElseGet(() -> {
                log.warn("No report available for {} to notify", date);
                return null;
            });
        if (report == null) {
            return;
        }

        ReportReadyNotificationRequest payload = ReportReadyNotificationRequest.from(report);

        try {
            notificationsClient.notifyReportReady(notificationsBaseUrl, internalApiKey, payload)
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(ex -> {
                    log.warn("Failed to send notification for {}: {}", date, ex.getMessage());
                    return Mono.empty();
                })
                .blockOptional();
        } catch (Exception ex) {
            log.warn("Notification error for {}", date, ex);
        }
    }

    public ReporterStatus status() {
        Instant lastIngested = newsItemRepository.findTop1ByOrderByIngestedAtDesc()
            .map(NewsItem::getIngestedAt)
            .orElse(null);
        LocalDate lastReport = dailyReportRepository.findTop1ByOrderByReportDateDesc()
            .map(DailyReport::getReportDate)
            .orElse(null);
        return new ReporterStatus(lastIngested, lastReport);
    }

    public record ReporterStatus(Instant lastIngestedAt, LocalDate lastReportDate) {
    }
}
