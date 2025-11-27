package com.novareport.accounts_service.settings;

import com.novareport.accounts_service.settings.dto.ReportEmailSubscriberResponse;
import com.novareport.accounts_service.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalUserSettingsControllerTest {

    @Mock
    private UserSettingsRepository userSettingsRepository;

    private InternalUserSettingsController controller;

    @BeforeEach
    void setUp() {
        controller = new InternalUserSettingsController(userSettingsRepository);
    }

    @Test
    void getReportEmailSubscribersMapsEntitiesToResponse() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("hash")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        UserSettings settings = UserSettings.builder()
                .user(user)
                .reportEmailOptIn(true)
                .build();

        when(userSettingsRepository.findByReportEmailOptInTrue()).thenReturn(List.of(settings));

        List<ReportEmailSubscriberResponse> result = controller.getReportEmailSubscribers();

        assertThat(result).hasSize(1);
        ReportEmailSubscriberResponse response = result.get(0);
        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.email()).isEqualTo(user.getEmail());
    }

    @Test
    void getReportEmailSubscribersExcludesUsersWithoutOptIn() {
        User optInUser = User.builder()
                .id(UUID.randomUUID())
                .email("optin@example.com")
                .passwordHash("hash")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User optOutUser = User.builder()
                .id(UUID.randomUUID())
                .email("optout@example.com")
                .passwordHash("hash")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        UserSettings optInSettings = UserSettings.builder()
                .user(optInUser)
                .reportEmailOptIn(true)
                .build();

        UserSettings optOutSettings = UserSettings.builder()
                .user(optOutUser)
                .reportEmailOptIn(false)
                .build();

        when(userSettingsRepository.findByReportEmailOptInTrue())
                .thenReturn(List.of(optInSettings, optOutSettings));

        List<ReportEmailSubscriberResponse> result = controller.getReportEmailSubscribers();

        assertThat(result).hasSize(1);
        ReportEmailSubscriberResponse response = result.get(0);
        assertThat(response.userId()).isEqualTo(optInUser.getId());
        assertThat(response.email()).isEqualTo(optInUser.getEmail());
    }
}
