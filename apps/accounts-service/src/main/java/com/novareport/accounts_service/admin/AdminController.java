package com.novareport.accounts_service.admin;

import com.novareport.accounts_service.settings.UserSettings;
import com.novareport.accounts_service.settings.UserSettingsRepository;
import com.novareport.accounts_service.user.User;
import com.novareport.accounts_service.user.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping({"/api/accounts/admin", "/admin"})
@Tag(name = "Admin", description = "Admin operations for accounts service")
public class AdminController {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final MeterRegistry meterRegistry;

    public AdminController(
        UserRepository userRepository,
        UserSettingsRepository userSettingsRepository,
        MeterRegistry meterRegistry
    ) {
        this.userRepository = userRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/users/by-email")
    @Operation(summary = "Find user by email", description = "Returns basic user and settings info for a given email")
    public ResponseEntity<AdminUserDetailsResponse> getUserByEmail(
        @RequestParam("email") @Email @NotBlank String email
    ) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        UUID userId = Objects.requireNonNull(user.getId(), "userId must not be null");
        Optional<UserSettings> settingsOpt = userSettingsRepository.findById(userId);

        AdminUserSettings settings = settingsOpt
            .map(s -> new AdminUserSettings(
                s.getLocale(),
                s.getTimezone(),
                Boolean.TRUE.equals(s.getMarketingOptIn()),
                Boolean.TRUE.equals(s.getReportEmailOptIn()),
                Boolean.TRUE.equals(s.getTwoFactorEnabled())
            ))
            .orElseGet(() -> new AdminUserSettings(
                "sv-SE",
                "Europe/Stockholm",
                false,
                false,
                false
            ));

        AdminUserDetailsResponse response = new AdminUserDetailsResponse(
            userId,
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole(),
            Boolean.TRUE.equals(user.getIsActive()),
            user.getCreatedAt(),
            settings
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{userId}/anonymize")
    @Operation(summary = "Anonymize user and free email", description = "Archives the user's email so the original address can be reused. Marks the account as inactive.")
    public ResponseEntity<AdminUserDetailsResponse> anonymizeUser(@PathVariable("userId") UUID userId) {
        UUID effectiveUserId = Objects.requireNonNull(userId, "userId must not be null");
        Optional<User> userOpt = userRepository.findById(effectiveUserId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        String originalEmail = user.getEmail();
        if (originalEmail == null || originalEmail.isBlank()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        String archivedEmail = buildArchivedEmail(originalEmail);
        user.setEmail(archivedEmail);
        user.setIsActive(false);
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);

        Optional<UserSettings> settingsOpt = userSettingsRepository.findById(effectiveUserId);
        AdminUserSettings settings = settingsOpt
            .map(s -> new AdminUserSettings(
                s.getLocale(),
                s.getTimezone(),
                Boolean.TRUE.equals(s.getMarketingOptIn()),
                Boolean.TRUE.equals(s.getReportEmailOptIn()),
                Boolean.TRUE.equals(s.getTwoFactorEnabled())
            ))
            .orElseGet(() -> new AdminUserSettings(
                "sv-SE",
                "Europe/Stockholm",
                false,
                false,
                false
            ));

        AdminUserDetailsResponse response = new AdminUserDetailsResponse(
            saved.getId(),
            saved.getEmail(),
            saved.getFirstName(),
            saved.getLastName(),
            saved.getRole(),
            Boolean.TRUE.equals(saved.getIsActive()),
            saved.getCreatedAt(),
            settings
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{userId}/settings")
    @Operation(summary = "Update user settings (admin)", description = "Allows admin to update locale, timezone and email preferences for a user")
    public ResponseEntity<AdminUserDetailsResponse> updateUserSettings(
        @PathVariable("userId") UUID userId,
        @Valid @RequestBody AdminUpdateUserSettingsRequest request
    ) {
        UUID effectiveUserId = Objects.requireNonNull(userId, "userId must not be null");
        Optional<User> userOpt = userRepository.findById(effectiveUserId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();

        UserSettings settings = userSettingsRepository.findById(effectiveUserId)
            .orElseGet(() -> UserSettings.builder()
                .userId(effectiveUserId)
                .user(user)
                .build()
            );

        settings.setLocale(request.locale());
        settings.setTimezone(request.timezone());
        settings.setMarketingOptIn(request.marketingOptIn());
        settings.setReportEmailOptIn(request.reportEmailOptIn());
        settings.setTwoFactorEnabled(request.twoFactorEnabled());

        userSettingsRepository.save(settings);

        AdminUserSettings adminSettings = new AdminUserSettings(
            settings.getLocale(),
            settings.getTimezone(),
            Boolean.TRUE.equals(settings.getMarketingOptIn()),
            Boolean.TRUE.equals(settings.getReportEmailOptIn()),
            Boolean.TRUE.equals(settings.getTwoFactorEnabled())
        );

        AdminUserDetailsResponse response = new AdminUserDetailsResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole(),
            Boolean.TRUE.equals(user.getIsActive()),
            user.getCreatedAt(),
            adminSettings
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/metrics")
    @Operation(summary = "Accounts metrics", description = "Returns aggregated metrics for authentication and user counts")
    public AdminMetricsResponse getMetrics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();

        double loginSuccess = getCounterValue("nova_auth_logins_total", "success");
        double loginInvalid = getCounterValue("nova_auth_logins_total", "invalid_credentials");
        double loginError = getCounterValue("nova_auth_logins_total", "error");

        AdminLoginMetrics loginMetrics = new AdminLoginMetrics(
            loginSuccess,
            loginInvalid,
            loginError,
            getTimerMeanMillis("nova_auth_login_latency_seconds", "success"),
            getTimerMeanMillis("nova_auth_login_latency_seconds", "invalid_credentials"),
            getTimerMeanMillis("nova_auth_login_latency_seconds", "error")
        );

        return new AdminMetricsResponse(totalUsers, activeUsers, loginMetrics);
    }

    private double getCounterValue(String name, String outcome) {
        var counter = meterRegistry.find(name).tag("outcome", outcome).counter();
        return counter != null ? counter.count() : 0.0;
    }

    private double getTimerMeanMillis(String name, String outcome) {
        Timer timer = meterRegistry.find(name).tag("outcome", outcome).timer();
        if (timer == null || timer.count() == 0) {
            return 0.0;
        }
        return timer.mean(TimeUnit.MILLISECONDS);
    }

    private String buildArchivedEmail(String email) {
        String trimmed = email.trim();
        int atIndex = trimmed.indexOf('@');
        String localPart;
        String domain;
        if (atIndex > 0 && atIndex < trimmed.length() - 1) {
            localPart = trimmed.substring(0, atIndex);
            domain = trimmed.substring(atIndex + 1);
        } else {
            localPart = trimmed;
            domain = "invalid.local";
        }
        String suffix = "+archive-" + Instant.now().toEpochMilli();
        return localPart + suffix + "@" + domain.toLowerCase(Locale.ROOT);
    }

    public record AdminUserDetailsResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        boolean active,
        Instant createdAt,
        AdminUserSettings settings
    ) {
    }

    public record AdminUserSettings(
        String locale,
        String timezone,
        boolean marketingOptIn,
        boolean reportEmailOptIn,
        boolean twoFactorEnabled
    ) {
    }

    public record AdminUpdateUserSettingsRequest(
        @NotBlank String locale,
        @NotBlank String timezone,
        Boolean marketingOptIn,
        Boolean reportEmailOptIn,
        Boolean twoFactorEnabled
    ) {
    }

    public record AdminLoginMetrics(
        double success,
        double invalidCredentials,
        double error,
        double meanLatencySuccessMs,
        double meanLatencyInvalidCredentialsMs,
        double meanLatencyErrorMs
    ) {
    }

    public record AdminMetricsResponse(
        long totalUsers,
        long activeUsers,
        AdminLoginMetrics logins
    ) {
    }
}
