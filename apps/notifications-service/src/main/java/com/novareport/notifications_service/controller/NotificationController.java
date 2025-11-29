package com.novareport.notifications_service.controller;

import com.novareport.notifications_service.dto.NotificationReportResponse;
import com.novareport.notifications_service.dto.PaymentConfirmationEmailRequest;
import com.novareport.notifications_service.dto.ReportReadyNotificationRequest;
import com.novareport.notifications_service.dto.WelcomeEmailRequest;
import com.novareport.notifications_service.service.NotificationReportService;
import com.novareport.notifications_service.service.PaymentConfirmationEmailService;
import com.novareport.notifications_service.service.WelcomeEmailService;
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
    private final WelcomeEmailService welcomeEmailService;
    private final PaymentConfirmationEmailService paymentConfirmationEmailService;

    public NotificationController(NotificationReportService reportService, WelcomeEmailService welcomeEmailService, PaymentConfirmationEmailService paymentConfirmationEmailService) {
        this.reportService = reportService;
        this.welcomeEmailService = welcomeEmailService;
        this.paymentConfirmationEmailService = paymentConfirmationEmailService;
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

    @PostMapping("/internal/notifications/welcome-email")
    public ResponseEntity<Void> sendWelcomeEmail(@Valid @RequestBody WelcomeEmailRequest request) {
        welcomeEmailService.sendWelcomeEmail(request.email(), request.firstName());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/internal/notifications/payment-confirmed-email")
    public ResponseEntity<Void> sendPaymentConfirmedEmail(@Valid @RequestBody PaymentConfirmationEmailRequest request) {
        paymentConfirmationEmailService.sendPaymentConfirmedEmail(request.userId(), request.plan(), request.durationDays());
        return ResponseEntity.accepted().build();
    }
}
