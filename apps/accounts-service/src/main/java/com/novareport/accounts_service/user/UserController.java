package com.novareport.accounts_service.user;

import com.novareport.accounts_service.settings.UserSettingsRepository;
import com.novareport.accounts_service.settings.dto.UpdateSettingsRequest;
import com.novareport.accounts_service.user.dto.UserProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserRepository users;
    private final UserSettingsRepository settings;

    @GetMapping("/me")
    public UserProfileResponse me(Authentication auth) {
        var user = users.findByEmail(auth.getName())
            .orElseThrow(() -> new java.util.NoSuchElementException("User not found with email: " + auth.getName()));
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole());
    }

    @PutMapping("/me/settings")
    public void updateSettings(Authentication auth, @Valid @RequestBody UpdateSettingsRequest req) {
        var user = users.findByEmail(auth.getName())
            .orElseThrow(() -> new java.util.NoSuchElementException("User not found with email: " + auth.getName()));
        var s = settings.findById(user.getId()).orElseThrow(() -> new java.util.NoSuchElementException("User settings not found for user ID: " + user.getId()));
        s.setLocale(req.locale());
        s.setTimezone(req.timezone());
        s.setMarketingOptIn(Boolean.TRUE.equals(req.marketingOptIn()));
        s.setTwoFactorEnabled(Boolean.TRUE.equals(req.twoFactorEnabled()));
        settings.save(s);
    }
}
