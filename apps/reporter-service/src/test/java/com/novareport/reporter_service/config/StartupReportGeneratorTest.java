package com.novareport.reporter_service.config;

import com.novareport.reporter_service.domain.DailyReport;
import com.novareport.reporter_service.service.ReporterCoordinator;
import com.novareport.reporter_service.service.RssIngestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StartupReportGeneratorTest {

    private ReporterProperties reporterProperties;
    private ReporterCoordinator coordinator;
    private StartupReportGenerator config;

    @BeforeEach
    void setUp() {
        reporterProperties = mock(ReporterProperties.class);
        coordinator = mock(ReporterCoordinator.class);
        config = new StartupReportGenerator();
    }

    @Test
    void reportStartupRunnerDoesNothingWhenStartupGenerateReportFalse() throws Exception {
        when(reporterProperties.startupGenerateReport()).thenReturn(false);

        var runner = config.reportStartupRunner(reporterProperties, coordinator);

        runner.run(new DefaultApplicationArguments(new String[0]));

        verifyNoInteractions(coordinator);
    }

    @Test
    void reportStartupRunnerIngestsAndBuildsReportWhenEnabled() throws Exception {
        when(reporterProperties.startupGenerateReport()).thenReturn(true);

        RssIngestService.IngestResult ingestResult = new RssIngestService.IngestResult(10, 8);
        when(coordinator.ingestNow()).thenReturn(ingestResult);

        DailyReport report = new DailyReport();
        report.setId(UUID.randomUUID());
        report.setReportDate(LocalDate.now());
        report.setSummary("summary");
        report.setCreatedAt(Instant.now());
        when(coordinator.buildReport(any(LocalDate.class))).thenReturn(report);

        var runner = config.reportStartupRunner(reporterProperties, coordinator);

        assertThatCode(() -> runner.run(new DefaultApplicationArguments(new String[0])))
            .doesNotThrowAnyException();

        verify(coordinator).ingestNow();
        verify(coordinator).buildReport(any(LocalDate.class));
    }
}
