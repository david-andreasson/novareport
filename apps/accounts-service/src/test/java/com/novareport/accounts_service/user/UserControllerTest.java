package com.novareport.accounts_service.user;

import com.novareport.accounts_service.settings.UserSettings;
import com.novareport.accounts_service.settings.UserSettingsRepository;
import com.novareport.accounts_service.settings.dto.UpdateSettingsRequest;
import com.novareport.accounts_service.user.dto.UserProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private Authentication authentication;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(userRepository, userSettingsRepository);
    }

    @Test
    void meReturnsCurrentUserProfile() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .email("user@example.com")
                .passwordHash("hash")
                .firstName("First")
                .lastName("Last")
                .role("USER")
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(authentication.getName()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        UserProfileResponse response = controller.me(authentication);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.firstName()).isEqualTo("First");
        assertThat(response.lastName()).isEqualTo("Last");
        assertThat(response.role()).isEqualTo("USER");
    }

    @Test
    void meThrowsWhenUserNotFound() {
        when(authentication.getName()).thenReturn("missing@example.com");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.me(authentication))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void updateSettingsUpdatesValuesAndSaves() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .email("user@example.com")
                .passwordHash("hash")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        UserSettings settings = UserSettings.builder()
                .user(user)
                .build();

        when(authentication.getName()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userSettingsRepository.findById(Objects.requireNonNull(id))).thenReturn(Optional.of(settings));

        UpdateSettingsRequest request = new UpdateSettingsRequest(
                "en-US",
                "Europe/Stockholm",
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE
        );

        controller.updateSettings(authentication, request);

        assertThat(settings.getLocale()).isEqualTo("en-US");
        assertThat(settings.getTimezone()).isEqualTo("Europe/Stockholm");
        assertThat(settings.getMarketingOptIn()).isTrue();
        assertThat(settings.getReportEmailOptIn()).isTrue();
        assertThat(settings.getTwoFactorEnabled()).isTrue();
        verify(userSettingsRepository).save(settings);
    }

    @Test
    void updateSettingsThrowsWhenSettingsMissing() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .email("user@example.com")
                .passwordHash("hash")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(authentication.getName()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userSettingsRepository.findById(Objects.requireNonNull(id))).thenReturn(Optional.empty());

        UpdateSettingsRequest request = new UpdateSettingsRequest(
                "en-US",
                "Europe/Stockholm",
                null,
                null,
                null
        );

        assertThatThrownBy(() -> controller.updateSettings(authentication, request))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }
}
