package com.novareport.notifications_service.service;

import com.novareport.notifications_service.domain.NotificationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ReportEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private ReportEmailService service;

    @BeforeEach
    void setUp() {
        service = new ReportEmailService(mailSender, "from@test.local");
    }

    @Test
    void sendDailyReportBuildsAndSendsEmail() {
        NotificationReport report = new NotificationReport();
        report.setReportDate(LocalDate.of(2025, 1, 1));
        report.setSummary(
            "# Title\n\n" +
            "Executive Summary\n" +
            "This is **bold**, this is *italic*, and this is `code`.\n"
        );

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        service.sendDailyReport("user@example.com", report);

        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();

        assertThat(message.getFrom()).isEqualTo("from@test.local");
        assertThat(message.getTo()).containsExactly("user@example.com");
        assertThat(message.getSubject()).contains("Daily report 2025-01-01");
        assertThat(message.getText()).contains("Title");
        assertThat(message.getText()).contains("This is bold, this is italic, and this is code.");
        assertThat(message.getText()).doesNotContain("**");
        assertThat(message.getText()).doesNotContain("`");
    }
}
