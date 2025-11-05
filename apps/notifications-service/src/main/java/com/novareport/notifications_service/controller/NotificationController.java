package com.novareport.notifications_service.controller;

import com.novareport.notifications_service.dto.NotificationReportResponse;
import com.novareport.notifications_service.dto.ReportReadyNotificationRequest;
import com.novareport.notifications_service.service.NotificationReportService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class NotificationController {

    private final NotificationReportService reportService;

    public NotificationController(NotificationReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/internal/notifications/report-ready")
    public ResponseEntity<Void> reportReady(@Valid @RequestBody ReportReadyNotificationRequest request) {
        reportService.upsertReport(request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/notifications/latest")
    public ResponseEntity<NotificationReportResponse> latest() {
        return reportService.findLatest()
            .map(NotificationReportResponse::from)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
