package com.novareport.accounts_service.user.dto;

import java.util.UUID;

public record UserProfileResponse(UUID id, String email, String firstName, String lastName, String role) {
}
