package com.novareport.subscriptions_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtService {

    private final SecretKey key;
    private final String issuer;

    public JwtService(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.issuer}") String issuer
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.issuer = issuer;
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .requireIssuer(issuer)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
