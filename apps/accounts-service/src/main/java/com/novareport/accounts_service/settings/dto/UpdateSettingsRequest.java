package com.novareport.accounts_service.settings.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateSettingsRequest(
        @NotBlank String locale,
        @NotBlank String timezone,
        Boolean marketingOptIn,
        Boolean twoFactorEnabled
) {
}
