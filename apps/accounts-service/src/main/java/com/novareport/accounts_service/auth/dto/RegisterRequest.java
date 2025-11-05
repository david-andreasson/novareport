package com.novareport.accounts_service.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,}$",
                message = "Password must be at least 8 characters and include one uppercase letter and one special character"
        )
        String password,
        @NotBlank String firstName,
        @NotBlank String lastName
) {
}
