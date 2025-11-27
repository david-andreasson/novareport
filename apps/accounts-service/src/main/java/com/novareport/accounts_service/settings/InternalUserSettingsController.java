package com.novareport.accounts_service.settings;

import com.novareport.accounts_service.settings.dto.ReportEmailSubscriberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts/internal")
@RequiredArgsConstructor
public class InternalUserSettingsController {

    private final UserSettingsRepository settingsRepository;

    @GetMapping("/report-email-subscribers")
    public List<ReportEmailSubscriberResponse> getReportEmailSubscribers() {
        return settingsRepository.findByReportEmailOptInTrue().stream()
            .filter(settings -> Boolean.TRUE.equals(settings.getReportEmailOptIn()))
            .map(settings -> new ReportEmailSubscriberResponse(
                settings.getUser().getId(),
                settings.getUser().getEmail()
            ))
            .collect(Collectors.toList());
    }
}
