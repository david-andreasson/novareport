package com.novareport.reporter_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Scheduled job that generates reports every 4 hours.
 * Schedule: 00:00, 04:00, 08:00, 12:00, 16:00, 20:00
 */
@Service
public class ScheduledReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(ScheduledReportGenerator.class);

    private final ReporterCoordinator coordinator;

    public ScheduledReportGenerator(ReporterCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Generates reports every 4 hours: 00:00, 04:00, 08:00, 12:00, 16:00, 20:00
     * Cron format: second minute hour day month weekday
     * "0 0 0/4 * * *" means: at second 0, minute 0, every 4th hour
     */
    @Scheduled(cron = "0 0 0/4 * * *")
    public void generateScheduledReport() {
        log.info("=== Starting scheduled report generation ===");
        
        try {
            // Step 1: Ingest latest RSS feeds
            log.info("Step 1: Ingesting RSS feeds...");
            RssIngestService.IngestResult ingestResult = coordinator.ingestNow();
            log.info("RSS ingest complete - Attempted: {}, Stored: {}, Duplicates: {}", 
                ingestResult.attempted(), ingestResult.stored(), 
                ingestResult.attempted() - ingestResult.stored());

            // Step 2: Build report for today
            LocalDate today = LocalDate.now();
            log.info("Step 2: Building report for {}...", today);
            var report = coordinator.buildReport(today);
            log.info("Report generated successfully - ID: {}, Summary length: {} chars", 
                report.getId(), report.getSummary().length());

            log.info("=== Scheduled report generation complete ===");

        } catch (Exception e) {
            log.error("Failed to generate scheduled report", e);
            // Don't rethrow - we want the scheduler to continue running
        }
    }
}
