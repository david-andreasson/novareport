package com.novareport.notifications_service.dto;

import java.util.UUID;

public record UserContactResponse(
    UUID id,
    String email,
    String firstName
) {
}
