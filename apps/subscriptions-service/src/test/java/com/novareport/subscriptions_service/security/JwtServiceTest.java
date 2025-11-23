package com.novareport.subscriptions_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void parseReturnsClaimsForValidToken() {
        String secret = "01234567890123456789012345678901"; // 32 chars
        String issuer = "nova-report";
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

        String token = Jwts.builder()
                .setSubject("user1")
                .claim("role", "ADMIN")
                .claim("uid", "uid-123")
                .setIssuer(issuer)
                .signWith(key)
                .compact();

        JwtService service = new JwtService(secret, issuer);

        Claims claims = service.parse(token);

        assertThat(claims.getSubject()).isEqualTo("user1");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.get("uid", String.class)).isEqualTo("uid-123");
    }
}
