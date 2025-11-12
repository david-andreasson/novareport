package com.novareport.accounts_service.config;

import com.novareport.accounts_service.settings.UserSettings;
import com.novareport.accounts_service.settings.UserSettingsRepository;
import com.novareport.accounts_service.user.User;
import com.novareport.accounts_service.user.UserRepository;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProdDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProdDataSeeder.class);

    private static final UUID ACTIVE_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID NO_PLAN_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final String ACTIVE_USER_EMAIL = "active.user@example.com";
    private static final String NO_PLAN_USER_EMAIL = "noplan.user@example.com";
    private static final String SEEDED_PASSWORD = "Password123!";

    private final UserRepository users;
    private final UserSettingsRepository settings;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUser(
            ACTIVE_USER_ID,
            ACTIVE_USER_EMAIL,
            "Aktiv",
            "Användare"
        );
        seedUser(
            NO_PLAN_USER_ID,
            NO_PLAN_USER_EMAIL,
            "Prenumerant",
            "Saknas"
        );
    }

    private void seedUser(UUID id, String email, String firstName, String lastName) {
        if (users.existsById(id)) {
            log.info("Prod test user {} already exists, skipping", email);
            return;
        }
        
        User user = users.findByEmail(email).orElseGet(() -> {
            log.info("Seeding prod test user {}", email);
            User created = Objects.requireNonNull(User.builder()
                .id(id)
                .email(email)
                .passwordHash(passwordEncoder.encode(SEEDED_PASSWORD))
                .firstName(firstName)
                .lastName(lastName)
                .build());
            return users.save(created);
        });

        UUID userId = Objects.requireNonNull(user.getId(), "Seeded user must have id");
        if (!settings.existsById(userId)) {
            log.info("Seeding settings for prod test user {}", email);
            UserSettings createdSettings = Objects.requireNonNull(UserSettings.builder()
                .user(user)
                .build());
            settings.save(createdSettings);
        }
    }
}
