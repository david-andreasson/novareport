package com.novareport.reporter_service.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface DailyReportRepository extends JpaRepository<DailyReport, UUID> {

    Optional<DailyReport> findByReportDate(LocalDate reportDate);

    Optional<DailyReport> findTop1ByOrderByReportDateDesc();

    Page<DailyReport> findAllByReportDateBetweenOrderByReportDateDesc(LocalDate from, LocalDate to, Pageable pageable);
}
