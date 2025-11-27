package com.novareport.notifications_service.controller;

import com.novareport.notifications_service.domain.NotificationReport;
import com.novareport.notifications_service.dto.NotificationReportResponse;
import com.novareport.notifications_service.dto.ReportReadyNotificationRequest;
import com.novareport.notifications_service.dto.WelcomeEmailRequest;
import com.novareport.notifications_service.service.NotificationReportService;
import com.novareport.notifications_service.service.WelcomeEmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationReportService reportService;

    @Mock
    private WelcomeEmailService welcomeEmailService;

    @InjectMocks
    private NotificationController controller;

    @Test
    void reportReadyReturnsAccepted() {
        ReportReadyNotificationRequest request = new ReportReadyNotificationRequest(
                UUID.randomUUID(),
                LocalDate.now(),
                "summary"
        );

        ResponseEntity<Void> response = controller.reportReady(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(reportService).upsertReport(request);
    }

    @Test
    void latestReturnsOkWhenReportExists() {
        NotificationReport report = new NotificationReport();
        when(reportService.findLatest()).thenReturn(Optional.of(report));

        ResponseEntity<NotificationReportResponse> response = controller.latest();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void latestReturnsNotFoundWhenNoReport() {
        when(reportService.findLatest()).thenReturn(Optional.empty());

        ResponseEntity<NotificationReportResponse> response = controller.latest();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void sendWelcomeEmailReturnsAcceptedAndDelegatesToService() {
        WelcomeEmailRequest request = new WelcomeEmailRequest("user@example.com", "Anna");

        ResponseEntity<Void> response = controller.sendWelcomeEmail(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(welcomeEmailService).sendWelcomeEmail("user@example.com", "Anna");
    }
}
