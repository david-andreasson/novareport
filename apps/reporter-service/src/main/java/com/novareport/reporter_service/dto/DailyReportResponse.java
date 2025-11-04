package com.novareport.reporter_service.dto;

import com.novareport.reporter_service.domain.DailyReport;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DailyReportResponse(
    UUID id,
    LocalDate reportDate,
    String summary,
    Instant createdAt
) {
    public static DailyReportResponse fromEntity(DailyReport report) {
        return new DailyReportResponse(
            report.getId(),
            report.getReportDate(),
            report.getSummary(),
            report.getCreatedAt()
        );
    }
}
