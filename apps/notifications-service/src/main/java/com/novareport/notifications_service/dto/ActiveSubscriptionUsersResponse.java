package com.novareport.notifications_service.dto;

import java.util.List;
import java.util.UUID;

public record ActiveSubscriptionUsersResponse(List<UUID> userIds) {

    public ActiveSubscriptionUsersResponse {
        userIds = userIds == null ? List.of() : List.copyOf(userIds);
    }
}
