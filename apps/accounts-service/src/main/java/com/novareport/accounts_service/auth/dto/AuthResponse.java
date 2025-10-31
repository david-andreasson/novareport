package com.novareport.accounts_service.auth.dto;

public record AuthResponse(String accessToken, String tokenType) {
    public AuthResponse(String token) {
        this(token, "Bearer");
    }
}
