package com.novareport.notifications_service.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface NotificationReportRepository extends JpaRepository<NotificationReport, UUID> {

    Optional<NotificationReport> findByReportId(UUID reportId);

    Optional<NotificationReport> findByReportDate(LocalDate reportDate);

    Optional<NotificationReport> findTop1ByOrderByReportDateDesc();
}
