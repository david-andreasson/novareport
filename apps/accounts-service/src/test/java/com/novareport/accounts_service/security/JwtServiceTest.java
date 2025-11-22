package com.novareport.accounts_service.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void createAccessTokenAndParseRoundTrip() {
        String secret = "0123456789ABCDEF0123456789ABCDEF";
        JwtService service = new JwtService(secret, "accounts-service", 15);

        UUID userId = UUID.randomUUID();
        String email = "user@example.com";
        String role = "ADMIN";

        String token = service.createAccessToken(userId, email, role);

        Claims claims = service.parse(token);

        assertThat(claims.getSubject()).isEqualTo(email);
        assertThat(claims.get("uid", String.class)).isEqualTo(userId.toString());
        assertThat(claims.get("role", String.class)).isEqualTo(role);
        assertThat(claims.getIssuer()).isEqualTo("accounts-service");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }
}
