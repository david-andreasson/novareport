package com.novareport.accounts_service.settings.dto;

import java.util.UUID;

public record ReportEmailSubscriberResponse(UUID userId, String email) {
}
