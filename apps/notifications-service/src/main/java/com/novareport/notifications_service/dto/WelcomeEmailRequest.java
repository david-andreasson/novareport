package com.novareport.notifications_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record WelcomeEmailRequest(
    @Email @NotBlank String email,
    @NotBlank String firstName
) {
}
