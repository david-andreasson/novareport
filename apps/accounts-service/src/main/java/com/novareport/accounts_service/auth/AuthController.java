package com.novareport.accounts_service.auth;

import com.novareport.accounts_service.activity.ActivityLog;
import com.novareport.accounts_service.activity.ActivityLogRepository;
import com.novareport.accounts_service.auth.dto.AuthResponse;
import com.novareport.accounts_service.auth.dto.LoginRequest;
import com.novareport.accounts_service.auth.dto.RegisterRequest;
import com.novareport.accounts_service.security.JwtService;
import com.novareport.accounts_service.settings.UserSettings;
import com.novareport.accounts_service.settings.UserSettingsRepository;
import com.novareport.accounts_service.user.User;
import com.novareport.accounts_service.user.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository users;
    private final UserSettingsRepository settingsRepo;
    private final ActivityLogRepository activity;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    @NonNull
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        if (users.existsByEmail(req.email())) {
            throw new IllegalStateException("Email already registered");
        }
        User user = users.save(createUser(req));

        UserSettings settings = createDefaultSettings(user);
        settingsRepo.save(settings);
        ActivityLog registerLog = createActivityLog(user, "REGISTER");
        activity.save(registerLog);
        String token = jwt.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        User user = users.findByEmail(req.email()).orElseThrow(() -> new IllegalStateException("Invalid credentials"));
        boolean validPassword = encoder.matches(req.password(), user.getPasswordHash());
        boolean activeAccount = user.getIsActive();
        if (!validPassword || !activeAccount) {
            throw new IllegalStateException("Invalid credentials");
        }
        ActivityLog loginLog = createActivityLog(user, "LOGIN");
        activity.save(loginLog);
        String token = jwt.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token);
    }

    @NonNull
    private User createUser(RegisterRequest req) {
        return User.builder()
                .email(req.email())
                .passwordHash(encoder.encode(req.password()))
                .firstName(req.firstName())
                .lastName(req.lastName())
                .build();
    }

    @NonNull
    private UserSettings createDefaultSettings(User user) {
        return UserSettings.builder()
                .user(user)
                .build();
    }

    @NonNull
    private ActivityLog createActivityLog(User user, String event) {
        return ActivityLog.builder()
                .user(user)
                .event(event)
                .build();
    }
}
