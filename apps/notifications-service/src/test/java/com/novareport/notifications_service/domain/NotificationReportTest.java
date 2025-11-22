package com.novareport.notifications_service.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationReportTest {

    @Test
    void onCreateSetsCreatedAndUpdatedAt() {
        NotificationReport report = new NotificationReport();

        report.onCreate();

        assertThat(report.getCreatedAt()).isNotNull();
        assertThat(report.getUpdatedAt()).isNotNull();
    }

    @Test
    void onUpdateUpdatesUpdatedAt() {
        NotificationReport report = new NotificationReport();

        report.onCreate();
        Instant createdAt = report.getCreatedAt();

        report.onUpdate();

        assertThat(report.getCreatedAt()).isEqualTo(createdAt);
        assertThat(report.getUpdatedAt()).isNotNull();
    }
}
