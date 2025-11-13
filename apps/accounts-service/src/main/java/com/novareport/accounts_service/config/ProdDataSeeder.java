package com.novareport.accounts_service.config;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private final JdbcTemplate jdbcTemplate;
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
        String passwordHash = passwordEncoder.encode(SEEDED_PASSWORD);

        int affected = jdbcTemplate.update(
            """
            INSERT INTO users (id, email, password_hash, first_name, last_name, role, is_active, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, 'USER', TRUE, NOW(), NOW())
            ON CONFLICT (id) DO UPDATE
              SET email = EXCLUDED.email,
                  password_hash = EXCLUDED.password_hash,
                  first_name = EXCLUDED.first_name,
                  last_name = EXCLUDED.last_name,
                  updated_at = NOW()
            """,
            id,
            email,
            passwordHash,
            firstName,
            lastName
        );

        if (affected > 0) {
            log.info("Ensured prod test user {} exists", email);
        }

        jdbcTemplate.update(
            """
            INSERT INTO user_settings (user_id, locale, timezone, marketing_opt_in, two_factor_enabled)
            VALUES (?, 'sv-SE', 'Europe/Stockholm', FALSE, FALSE)
            ON CONFLICT (user_id) DO UPDATE
              SET updated_at = NOW()
            """,
            id
        );
    }
}
