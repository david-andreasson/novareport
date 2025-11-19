package com.novareport.notifications_service.service;

import com.novareport.notifications_service.domain.NotificationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ReportEmailService {

    private static final Logger log = LoggerFactory.getLogger(ReportEmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public ReportEmailService(JavaMailSender mailSender,
                              @Value("${notifications.mail.from:noreply@novareport.local}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendDailyReport(String to, NotificationReport report) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(buildSubject(report));
        message.setText(buildBody(report));
        mailSender.send(message);
        log.info("Sent daily report email for {} to {}", report.getReportDate(), to);
    }

    private String buildSubject(NotificationReport report) {
        return "Nova Report – Daily report " + report.getReportDate();
    }

    private String buildBody(NotificationReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hi,\n\n");
        sb.append("Here is your daily Nova Report for ")
            .append(report.getReportDate())
            .append(".\n\n");
        sb.append(report.getSummary()).append("\n\n");
        sb.append("Best regards,\nNova Report");
        return sb.toString();
    }
}
