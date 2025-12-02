package com.novareport.accounts_service.admin;

import com.novareport.accounts_service.settings.UserSettings;
import com.novareport.accounts_service.settings.UserSettingsRepository;
import com.novareport.accounts_service.user.User;
import com.novareport.accounts_service.user.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Search search;

    @Mock
    private Counter counter;

    @Mock
    private Timer timer;

    private AdminController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminController(userRepository, userSettingsRepository, meterRegistry);
    }

    @Test
    void getUserByEmailReturnsUserWhenFound() {
        User user = createTestUser();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userSettingsRepository.findById(user.getId())).thenReturn(Optional.empty());

        var response = controller.getUserByEmail("test@example.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo("test@example.com");
    }

    @Test
    void getUserByEmailReturnsNotFoundWhenUserDoesNotExist() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        var response = controller.getUserByEmail("test@example.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void anonymizeUserSuccessfully() {
        User user = createTestUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userSettingsRepository.findById(user.getId())).thenReturn(Optional.empty());

        var response = controller.anonymizeUser(user.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void anonymizeUserReturnsNotFoundWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        var response = controller.anonymizeUser(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateUserSettingsSuccessfully() {
        User user = createTestUser();
        UserSettings settings = UserSettings.builder()
                .userId(user.getId())
                .user(user)
                .build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userSettingsRepository.findById(user.getId())).thenReturn(Optional.of(settings));
        when(userSettingsRepository.save(any(UserSettings.class))).thenReturn(settings);

        var request = new AdminController.AdminUpdateUserSettingsRequest(
                "en-US", "America/New_York", true, true, false
        );

        var response = controller.updateUserSettings(user.getId(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userSettingsRepository).save(any(UserSettings.class));
    }

    @Test
    void updateUserSettingsReturnsNotFoundWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        var request = new AdminController.AdminUpdateUserSettingsRequest(
                "en-US", "America/New_York", true, true, false
        );

        var response = controller.updateUserSettings(userId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getMetricsReturnsAggregatedData() {
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByIsActiveTrue()).thenReturn(80L);
        when(meterRegistry.find(anyString())).thenReturn(search);
        when(search.tag(anyString(), anyString())).thenReturn(search);
        when(search.counter()).thenReturn(counter);
        when(search.timer()).thenReturn(timer);
        when(counter.count()).thenReturn(50.0);
        when(timer.count()).thenReturn(10L);
        when(timer.mean(TimeUnit.MILLISECONDS)).thenReturn(100.0);

        var response = controller.getMetrics();

        assertThat(response).isNotNull();
        assertThat(response.totalUsers()).isEqualTo(100L);
        assertThat(response.activeUsers()).isEqualTo(80L);
        assertThat(response.logins()).isNotNull();
    }

    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRole("USER");
        user.setIsActive(true);
        user.setCreatedAt(Instant.now());
        return user;
    }
}
