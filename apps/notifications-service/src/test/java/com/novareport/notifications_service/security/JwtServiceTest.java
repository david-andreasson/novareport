package com.novareport.notifications_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void parseReturnsClaimsWhenTokenIsValid() {
        String secret = "01234567890123456789012345678901";
        String issuer = "test-issuer";
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        JwtService jwtService = new JwtService(secret, issuer);

        String token = Jwts.builder()
                .setSubject("subject-user")
                .setIssuer(issuer)
                .claim("role", "ADMIN")
                .claim("uid", "user-123")
                .signWith(key)
                .compact();

        Claims claims = jwtService.parse(token);

        assertThat(claims.getSubject()).isEqualTo("subject-user");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.get("uid", String.class)).isEqualTo("user-123");
    }
}
