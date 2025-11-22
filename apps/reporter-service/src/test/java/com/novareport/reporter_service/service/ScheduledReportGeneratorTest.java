package com.novareport.reporter_service.service;

import com.novareport.reporter_service.domain.DailyReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledReportGeneratorTest {

    private ReporterCoordinator coordinator;
    private ScheduledReportGenerator generator;

    @BeforeEach
    void setUp() {
        coordinator = mock(ReporterCoordinator.class);
        generator = new ScheduledReportGenerator(coordinator);
    }

    @Test
    void generateScheduledReportCallsCoordinator() {
        RssIngestService.IngestResult ingestResult = new RssIngestService.IngestResult(5, 3);
        when(coordinator.ingestNow()).thenReturn(ingestResult);

        DailyReport report = new DailyReport();
        report.setId(UUID.randomUUID());
        report.setReportDate(LocalDate.now());
        report.setSummary("summary");
        report.setCreatedAt(Instant.now());
        when(coordinator.buildReport(any(LocalDate.class))).thenReturn(report);

        generator.generateScheduledReport();

        verify(coordinator).ingestNow();
        verify(coordinator).buildReport(any(LocalDate.class));
    }

    @Test
    void generateScheduledReportSwallowsExceptions() {
        when(coordinator.ingestNow()).thenThrow(new RuntimeException("boom"));

        assertThatCode(() -> generator.generateScheduledReport())
            .doesNotThrowAnyException();
    }
}
