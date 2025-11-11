package com.novareport.reporter_service.controller;

import com.novareport.reporter_service.dto.DailyReportResponse;
import com.novareport.reporter_service.dto.IngestResponse;
import com.novareport.reporter_service.service.DailyReportService;
import com.novareport.reporter_service.service.ReporterCoordinator;
import com.novareport.reporter_service.service.ReporterStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/v1/internal/reporter")
@Tag(name = "Reporter Internal")
public class InternalReporterController {

    private final ReporterCoordinator coordinator;
    private final DailyReportService dailyReportService;

    public InternalReporterController(ReporterCoordinator coordinator, DailyReportService dailyReportService) {
        this.coordinator = coordinator;
        this.dailyReportService = dailyReportService;
    }

    @PostMapping("/ingest-now")
    @Operation(summary = "Trigger RSS ingest now")
    public ResponseEntity<IngestResponse> ingest() {
        var result = coordinator.ingestNow();
        return ResponseEntity.ok(IngestResponse.fromResult(result));
    }

    @PostMapping("/build-report")
    @Operation(summary = "Force build of report for given date")
    public ResponseEntity<Void> build(@RequestParam(name = "date") LocalDate date) {
        coordinator.buildReport(date);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/notify")
    @Operation(summary = "Trigger notification send for given date")
    public ResponseEntity<Void> notifyAgain(@RequestParam(name = "date") LocalDate date) {
        coordinator.notifyReportReady(date);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/status")
    @Operation(summary = "Get reporter service status")
    public ResponseEntity<ReporterStatus> status() {
        return ResponseEntity.ok(coordinator.status());
    }

    @GetMapping("/latest-report")
    @Operation(summary = "Get latest report (internal, no auth required)")
    public ResponseEntity<DailyReportResponse> latestReport() {
        return dailyReportService.findLatest()
            .map(DailyReportResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
