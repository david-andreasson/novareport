package com.novareport.notifications_service.service;

import com.novareport.notifications_service.domain.NotificationReport;
import com.novareport.notifications_service.domain.NotificationReportRepository;
import com.novareport.notifications_service.dto.ReportReadyNotificationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class NotificationReportService {

    private final NotificationReportRepository repository;

    public NotificationReportService(NotificationReportRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public NotificationReport upsertReport(ReportReadyNotificationRequest request) {
        NotificationReport report = repository.findByReportId(request.reportId())
            .orElseGet(() -> repository.findByReportDate(request.reportDate()).orElseGet(NotificationReport::new));

        report.setReportId(request.reportId());
        report.setReportDate(request.reportDate());
        report.setSummary(request.summary());

        return repository.save(report);
    }

    @Transactional(readOnly = true)
    public Optional<NotificationReport> findLatest() {
        return repository.findTop1ByOrderByReportDateDesc();
    }
}
