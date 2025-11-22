package com.novareport.accounts_service.settings;

import com.novareport.accounts_service.user.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserSettingsTest {

    @Test
    void builderSetsDefaultsAndUser() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("hash")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        UserSettings settings = UserSettings.builder()
                .user(user)
                .build();

        assertThat(settings.getUser()).isEqualTo(user);
        assertThat(settings.getUserId()).isNull();
        assertThat(settings.getLocale()).isEqualTo("sv-SE");
        assertThat(settings.getTimezone()).isEqualTo("Europe/Stockholm");
        assertThat(settings.getMarketingOptIn()).isFalse();
        assertThat(settings.getReportEmailOptIn()).isFalse();
        assertThat(settings.getTwoFactorEnabled()).isFalse();
    }
}
