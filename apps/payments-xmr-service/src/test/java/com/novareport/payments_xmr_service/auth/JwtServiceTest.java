package com.novareport.payments_xmr_service.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-minimum-32-bytes-long!!";
    private static final String ISSUER = "accounts-service";

    @Test
    void extractUserIdReturnsUidClaim() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .issuer(ISSUER)
                .claim("uid", "user-123")
                .signWith(key)
                .compact();

        JwtService jwtService = new JwtService(SECRET, ISSUER);

        String userId = jwtService.extractUserId(token);

        assertThat(userId).isEqualTo("user-123");
    }

    @Test
    void isTokenValidReturnsFalseForInvalidToken() {
        JwtService jwtService = new JwtService(SECRET, ISSUER);

        boolean valid = jwtService.isTokenValid("not-a-jwt");

        assertThat(valid).isFalse();
    }
}
