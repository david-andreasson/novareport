package com.novareport.accounts_service.user;

import com.novareport.accounts_service.settings.UserSettingsRepository;
import com.novareport.accounts_service.settings.dto.UpdateSettingsRequest;
import com.novareport.accounts_service.user.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/accounts", ""})
@Tag(name = "User Profile", description = "User profile and settings management")
public class UserController {

    private final UserRepository users;
    private final UserSettingsRepository settings;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns the authenticated user's profile information")
    public UserProfileResponse me(Authentication auth) {
        var user = users.findByEmail(auth.getName())
            .orElseThrow(() -> new java.util.NoSuchElementException("User not found with email: " + auth.getName()));
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole());
    }

    @PutMapping("/me/settings")
    @Operation(summary = "Update user settings", description = "Updates the authenticated user's settings")
    public void updateSettings(Authentication auth, @Valid @RequestBody UpdateSettingsRequest req) {
        var user = users.findByEmail(auth.getName())
            .orElseThrow(() -> new java.util.NoSuchElementException("User not found with email: " + auth.getName()));
        var userId = Objects.requireNonNull(user.getId(), "User ID must not be null");
        var s = settings.findById(userId).orElseThrow(() -> new java.util.NoSuchElementException("User settings not found for user ID: " + userId));
        s.setLocale(req.locale());
        s.setTimezone(req.timezone());
        s.setMarketingOptIn(Boolean.TRUE.equals(req.marketingOptIn()));
        s.setReportEmailOptIn(Boolean.TRUE.equals(req.reportEmailOptIn()));
        s.setTwoFactorEnabled(Boolean.TRUE.equals(req.twoFactorEnabled()));
        settings.save(s);
    }
}
