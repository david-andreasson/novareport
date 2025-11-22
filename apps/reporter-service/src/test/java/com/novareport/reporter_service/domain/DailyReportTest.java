package com.novareport.reporter_service.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DailyReportTest {

    @Test
    void onPersistSetsCreatedAtWhenNull() {
        DailyReport report = new DailyReport();
        report.setReportDate(LocalDate.of(2024, 1, 1));
        report.setSummary("summary");

        assertThat(report.getCreatedAt()).isNull();

        report.onPersist();

        assertThat(report.getCreatedAt()).isNotNull();
    }

    @Test
    void onPersistDoesNotOverrideExistingCreatedAt() {
        DailyReport report = new DailyReport();
        report.setReportDate(LocalDate.of(2024, 1, 1));
        report.setSummary("summary");
        Instant created = Instant.now().minusSeconds(60);
        report.setCreatedAt(created);

        report.onPersist();

        assertThat(report.getCreatedAt()).isEqualTo(created);
    }
}
