package com.novareport.accounts_service.auth;

import com.novareport.accounts_service.activity.ActivityLog;
import com.novareport.accounts_service.activity.ActivityLogRepository;
import com.novareport.accounts_service.auth.dto.AuthResponse;
import com.novareport.accounts_service.auth.dto.LoginRequest;
import com.novareport.accounts_service.auth.dto.RegisterRequest;
import com.novareport.accounts_service.common.exception.EmailAlreadyExistsException;
import com.novareport.accounts_service.common.exception.InvalidCredentialsException;
import com.novareport.accounts_service.security.JwtService;
import com.novareport.accounts_service.settings.UserSettings;
import com.novareport.accounts_service.settings.UserSettingsRepository;
import com.novareport.accounts_service.user.User;
import com.novareport.accounts_service.user.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private MeterRegistry meterRegistry;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        controller = new AuthController(userRepository, userSettingsRepository, activityLogRepository, passwordEncoder, jwtService, meterRegistry);
    }

    @Test
    void registerCreatesUserSettingsActivityAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("user@example.com", "Password!", "First", "Last");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password!")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setCreatedAt(Instant.now());
            u.setUpdatedAt(Instant.now());
            return u;
        });
        when(jwtService.createAccessToken(any(), anyString(), anyString())).thenReturn("token-123");

        AuthResponse response = controller.register(request);

        assertThat(response.accessToken()).isEqualTo("token-123");
        verify(userRepository).existsByEmail("user@example.com");
        verify(userRepository).save(any(User.class));
        verify(userSettingsRepository).save(any(UserSettings.class));
        verify(activityLogRepository).save(any(ActivityLog.class));
    }

    @Test
    void registerThrowsWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("user@example.com", "Password!", "First", "Last");
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> controller.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsTokenOnValidCredentialsAndActiveUser() {
        LoginRequest request = new LoginRequest("user@example.com", "secret");
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("encoded")
                .role("USER")
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
        when(jwtService.createAccessToken(user.getId(), user.getEmail(), user.getRole())).thenReturn("token-xyz");

        AuthResponse response = controller.login(request);

        assertThat(response.accessToken()).isEqualTo("token-xyz");
        verify(activityLogRepository).save(any(ActivityLog.class));
    }

    @Test
    void loginThrowsInvalidCredentialsWhenPasswordDoesNotMatch() {
        LoginRequest request = new LoginRequest("user@example.com", "wrong");
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("encoded")
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> controller.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(activityLogRepository, never()).save(any(ActivityLog.class));
    }

    @Test
    void loginThrowsInvalidCredentialsWhenUserInactive() {
        LoginRequest request = new LoginRequest("user@example.com", "secret");
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("encoded")
                .isActive(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);

        assertThatThrownBy(() -> controller.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(activityLogRepository, never()).save(any(ActivityLog.class));
    }
}
