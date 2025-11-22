package com.novareport.notifications_service.service;

import com.novareport.notifications_service.client.DiscordClient;
import com.novareport.notifications_service.domain.NotificationReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscordReportServiceTest {

    @Mock
    private DiscordClient discordClient;

    @InjectMocks
    private DiscordReportService discordReportServiceDisabled =
            new DiscordReportService(discordClient, false, "https://discord.test/webhook");

    @InjectMocks
    private DiscordReportService discordReportServiceEnabledNoUrl =
            new DiscordReportService(discordClient, true, " ");

    @Test
    void sendDailyReportReturnsFalseWhenDisabled() {
        NotificationReport report = new NotificationReport();
        report.setReportDate(LocalDate.now());

        boolean result = discordReportServiceDisabled.sendDailyReport(report);

        assertThat(result).isFalse();
        verifyNoInteractions(discordClient);
    }

    @Test
    void sendDailyReportReturnsFalseWhenWebhookMissing() {
        NotificationReport report = new NotificationReport();
        report.setReportDate(LocalDate.now());

        boolean result = discordReportServiceEnabledNoUrl.sendDailyReport(report);

        assertThat(result).isFalse();
        verifyNoInteractions(discordClient);
    }

    @Test
    void sendDailyReportReturnsTrueOnSuccess() {
        DiscordReportService service = new DiscordReportService(discordClient, true, "https://discord.test/webhook");
        NotificationReport report = new NotificationReport();
        report.setReportDate(LocalDate.now());
        report.setSummary("## Executive Summary\nOne\n## Market Trends\nTwo");

        when(discordClient.sendPayload(eq("https://discord.test/webhook"), any()))
                .thenReturn(Mono.empty());

        boolean result = service.sendDailyReport(report);

        assertThat(result).isTrue();
        verify(discordClient).sendPayload(eq("https://discord.test/webhook"), any());
    }

    @Test
    void sendDailyReportReturnsFalseOnException() {
        DiscordReportService service = new DiscordReportService(discordClient, true, "https://discord.test/webhook");
        NotificationReport report = new NotificationReport();
        report.setReportDate(LocalDate.now());
        report.setSummary("Summary text");

        when(discordClient.sendPayload(eq("https://discord.test/webhook"), any()))
                .thenReturn(Mono.error(new RuntimeException("boom")));

        boolean result = service.sendDailyReport(report);

        assertThat(result).isFalse();
        verify(discordClient).sendPayload(eq("https://discord.test/webhook"), any());
    }
}
