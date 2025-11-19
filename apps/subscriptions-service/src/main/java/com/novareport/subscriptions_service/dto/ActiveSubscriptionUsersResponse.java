package com.novareport.subscriptions_service.dto;

import java.util.List;
import java.util.UUID;

public record ActiveSubscriptionUsersResponse(List<UUID> userIds) {
}
