package com.novareport.payments_xmr_service.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import com.novareport.payments_xmr_service.util.LogSanitizer;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class JwtService {

    private final SecretKey secretKey;
    private final String issuer;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(String token) {
        String userId = extractClaim(token, "uid");
        log.debug("Extracted user ID from token: {}", LogSanitizer.sanitize(userId));
        return userId;
    }

    public String extractClaim(String token, String claimName) {
        Claims claims = extractClaims(token);
        return claims.get(claimName, String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("Invalid token: {}", LogSanitizer.sanitize(e.getMessage()));
            return false;
        }
    }
}
