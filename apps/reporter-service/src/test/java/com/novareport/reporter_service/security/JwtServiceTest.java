package com.novareport.reporter_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void parseReturnsClaimsForValidToken() {
        String secret = "01234567890123456789012345678901";
        String issuer = "test-issuer";
        JwtService jwtService = new JwtService(secret, issuer);

        String token = Jwts.builder()
            .setSubject("test-user")
            .setIssuer(issuer)
            .claim("role", "USER")
            .claim("uid", "123")
            .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
            .compact();

        Claims claims = jwtService.parse(token);

        assertThat(claims.getSubject()).isEqualTo("test-user");
        assertThat(claims.getIssuer()).isEqualTo(issuer);
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.get("uid", String.class)).isEqualTo("123");
    }
}
