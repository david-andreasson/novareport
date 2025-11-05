package com.novareport.reporter_service.dto;

import com.novareport.reporter_service.domain.DailyReport;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record ReportReadyNotificationRequest(UUID reportId, LocalDate reportDate, String summary) {

    public ReportReadyNotificationRequest {
        Objects.requireNonNull(reportId, "reportId must not be null");
        Objects.requireNonNull(reportDate, "reportDate must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
    }

    public static ReportReadyNotificationRequest from(DailyReport report) {
        return new ReportReadyNotificationRequest(report.getId(), report.getReportDate(), report.getSummary());
    }
}
