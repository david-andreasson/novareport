package com.novareport.reporter_service.service;

import com.novareport.reporter_service.client.NotificationsClient;
import com.novareport.reporter_service.domain.DailyReport;
import com.novareport.reporter_service.domain.DailyReportRepository;
import com.novareport.reporter_service.dto.ReportReadyNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class ReportNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReportNotificationPublisher.class);

    private final NotificationsClient notificationsClient;
    private final DailyReportRepository dailyReportRepository;
    private final String internalApiKey;
    private final String notificationsBaseUrl;

    public ReportNotificationPublisher(
        NotificationsClient notificationsClient,
        DailyReportRepository dailyReportRepository,
        @Value("${internal.api-key:}") String internalApiKey,
        @Value("${notif.base-url:}") String notificationsBaseUrl
    ) {
        this.notificationsClient = notificationsClient;
        this.dailyReportRepository = dailyReportRepository;
        this.internalApiKey = internalApiKey;
        this.notificationsBaseUrl = notificationsBaseUrl;
    }

    public void publish(LocalDate reportDate) {
        Optional<DailyReport> report = dailyReportRepository.findByReportDate(reportDate);
        report.ifPresentOrElse(
            this::publish,
            () -> log.warn("No report available for {} to notify", reportDate)
        );
    }

    public void publish(DailyReport report) {
        if (!StringUtils.hasText(notificationsBaseUrl)) {
            log.info("Notification base URL not configured, skipping notify for {}", report.getReportDate());
            return;
        }
        if (!StringUtils.hasText(internalApiKey)) {
            log.warn("Internal API key missing, skipping notify for {}", report.getReportDate());
            return;
        }

        ReportReadyNotificationRequest payload = ReportReadyNotificationRequest.from(report);

        try {
            notificationsClient.notifyReportReady(notificationsBaseUrl, internalApiKey, payload)
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(ex -> {
                    log.warn("Failed to send notification for {}: {}", report.getReportDate(), ex.getMessage());
                    return Mono.empty();
                })
                .blockOptional();
        } catch (Exception ex) {
            log.warn("Notification error for {}", report.getReportDate(), ex);
        }
    }
}
