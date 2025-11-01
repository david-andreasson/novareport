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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        if (users.existsByEmail(req.email())) {
            throw new IllegalStateException("Email already registered");
        }
        User user = users.save(User.builder()
                .email(req.email())
                .passwordHash(encoder.encode(req.password()))
                .firstName(req.firstName())
                .lastName(req.lastName())
                .build());

        settingsRepo.save(UserSettings.builder().user(user).build());
        activity.save(ActivityLog.builder().user(user).event("REGISTER").build());
        String token = jwt.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        // Always use the same error message for authentication failures
        User user = users.findByEmail(req.email()).orElse(null);
        // Use a dummy password hash if user not found to mitigate timing attacks
        String passwordHash = (user != null) ? user.getPasswordHash() : "$2a$10$7EqJtq98hPqEX7fNZaFWoO5uG1FQvY8p1p5lD1FQvY8p1p5lD1FQv"; // bcrypt for "invalid"
        boolean isActive = (user != null) ? user.getIsActive() : false;
        if (!isActive || !encoder.matches(req.password(), passwordHash)) {
            throw new IllegalStateException("Invalid credentials");
        }
        activity.save(ActivityLog.builder().user(user).event("LOGIN").build());
        String token = jwt.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token);
    }
}
