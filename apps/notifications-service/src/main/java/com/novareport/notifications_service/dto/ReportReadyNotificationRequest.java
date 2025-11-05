package com.novareport.notifications_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ReportReadyNotificationRequest(
    @NotNull UUID reportId,
    @NotNull LocalDate reportDate,
    @NotBlank String summary
) {
}
