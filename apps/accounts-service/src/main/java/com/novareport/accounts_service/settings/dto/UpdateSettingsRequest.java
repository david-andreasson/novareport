package com.novareport.accounts_service.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateSettingsRequest(
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z]{2,8}(-[a-zA-Z0-9]{1,8})*$", message = "Invalid locale format. Use IETF BCP 47 language tag, e.g., 'sv-SE'.")
        String locale,
        @NotBlank
        @Pattern(regexp = "^[A-Za-z]+/[A-Za-z_]+$", message = "Invalid timezone format. Use IANA Time Zone identifier, e.g., 'Europe/Stockholm'.")
        String timezone,
        Boolean marketingOptIn,
        Boolean twoFactorEnabled
) {
}
