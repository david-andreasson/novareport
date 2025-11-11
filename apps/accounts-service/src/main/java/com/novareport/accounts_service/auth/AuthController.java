package com.novareport.accounts_service.auth;

import com.novareport.accounts_service.activity.ActivityEventType;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration and login")
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
    @Operation(summary = "Register new user", description = "Creates a new user account and returns JWT token")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        if (users.existsByEmail(req.email())) {
            throw new EmailAlreadyExistsException(req.email());
        }
        User user = users.save(createUser(req));

        UserSettings settings = createDefaultSettings(user);
        settingsRepo.save(settings);
        ActivityLog registerLog = createActivityLog(user, ActivityEventType.REGISTER);
        activity.save(registerLog);
        String token = jwt.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token);
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user and returns JWT token")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        User user = users.findByEmail(req.email())
            .orElseThrow(() -> InvalidCredentialsException.invalidCredentials());
        boolean validPassword = encoder.matches(req.password(), user.getPasswordHash());
        boolean activeAccount = user.getIsActive();
        if (!validPassword || !activeAccount) {
            throw InvalidCredentialsException.invalidCredentials();
        }
        ActivityLog loginLog = createActivityLog(user, ActivityEventType.LOGIN);
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
    private ActivityLog createActivityLog(User user, ActivityEventType event) {
        return ActivityLog.builder()
                .user(user)
                .event(event)
                .build();
    }
}
