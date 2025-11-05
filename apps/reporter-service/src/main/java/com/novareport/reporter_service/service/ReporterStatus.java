package com.novareport.reporter_service.service;

import java.time.Instant;
import java.time.LocalDate;

public record ReporterStatus(Instant lastIngestedAt, LocalDate lastReportDate) {
}
