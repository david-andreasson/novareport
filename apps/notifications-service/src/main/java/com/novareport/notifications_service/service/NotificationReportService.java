package com.novareport.notifications_service.service;

import com.novareport.notifications_service.domain.NotificationReport;
import com.novareport.notifications_service.domain.NotificationReportRepository;
import com.novareport.notifications_service.dto.ReportReadyNotificationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class NotificationReportService {

    private final NotificationReportRepository repository;
    private final DiscordReportService discordReportService;

    public NotificationReportService(
        NotificationReportRepository repository,
        DiscordReportService discordReportService
    ) {
        this.repository = repository;
        this.discordReportService = discordReportService;
    }

    @Transactional
    public NotificationReport upsertReport(ReportReadyNotificationRequest request) {
        NotificationReport report = repository.findByReportId(request.reportId())
            .orElseGet(() -> repository.findByReportDate(request.reportDate()).orElseGet(NotificationReport::new));

        report.setReportId(request.reportId());
        report.setReportDate(request.reportDate());
        report.setSummary(request.summary());

        NotificationReport saved = repository.save(report);

        boolean sent = discordReportService.sendDailyReport(saved);
        if (sent) {
            saved.setDiscordSentAt(Instant.now());
            saved = repository.save(saved);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<NotificationReport> findLatest() {
        return repository.findTop1ByOrderByReportDateDesc();
    }

    @Transactional(readOnly = true)
    public Optional<NotificationReport> findByReportDate(LocalDate date) {
        return repository.findByReportDate(date);
    }

    @Transactional
    public NotificationReport save(NotificationReport report) {
        return repository.save(report);
    }
}
