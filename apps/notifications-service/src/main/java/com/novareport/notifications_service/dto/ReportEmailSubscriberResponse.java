package com.novareport.notifications_service.dto;

import java.util.UUID;

public record ReportEmailSubscriberResponse(UUID userId, String email) {
}
