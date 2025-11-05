package com.novareport.reporter_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "daily_reports", indexes = {
    @Index(name = "idx_daily_reports_report_date", columnList = "report_date", unique = true)
})
public class DailyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "report_date", nullable = false, unique = true)
    private LocalDate reportDate;

    @NotBlank
    @Column(columnDefinition = "TEXT", nullable = false)
    private String summary;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onPersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
