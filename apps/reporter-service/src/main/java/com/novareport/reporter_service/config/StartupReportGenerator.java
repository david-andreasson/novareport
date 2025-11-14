package com.novareport.reporter_service.config;

import com.novareport.reporter_service.service.ReporterCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
public class StartupReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(StartupReportGenerator.class);

    @Bean
    public ApplicationRunner reportStartupRunner(ReporterProperties reporterProperties, ReporterCoordinator coordinator) {
        return args -> {
            if (!reporterProperties.startupGenerateReport()) {
                return;
            }

            LocalDate today = LocalDate.now();
            log.info("Startup report generation enabled - ingesting RSS and building report for {}", today);

            try {
                var ingestResult = coordinator.ingestNow();
                log.info("Startup RSS ingest complete - Attempted: {}, Stored: {}, Duplicates: {}",
                    ingestResult.attempted(), ingestResult.stored(),
                    ingestResult.attempted() - ingestResult.stored());

                var report = coordinator.buildReport(today);
                log.info("Startup report generated successfully - ID: {}, Summary length: {} chars",
                    report.getId(), report.getSummary().length());

            } catch (Exception e) {
                log.error("Startup report generation failed", e);
            }
        };
    }
}
