package com.novareport.notifications_service.dto;

import com.novareport.notifications_service.domain.NotificationReport;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record NotificationReportResponse(
    UUID reportId,
    LocalDate reportDate,
    String summary,
    Instant createdAt,
    Instant updatedAt
) {

    public static NotificationReportResponse from(NotificationReport report) {
        return new NotificationReportResponse(
            report.getReportId(),
            report.getReportDate(),
            report.getSummary(),
            report.getCreatedAt(),
            report.getUpdatedAt()
        );
    }
}
