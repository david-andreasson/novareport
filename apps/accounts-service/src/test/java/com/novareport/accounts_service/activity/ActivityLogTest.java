package com.novareport.accounts_service.activity;

import com.novareport.accounts_service.user.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityLogTest {

    @Test
    void onCreateSetsIdAndTimestampWhenMissing() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("hash")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ActivityLog log = ActivityLog.builder()
                .user(user)
                .event(ActivityEventType.LOGIN)
                .build();

        log.onCreate();

        assertThat(log.getId()).isNotNull();
        assertThat(log.getCreatedAt()).isNotNull();
    }

    @Test
    void onCreateKeepsExistingId() {
        UUID id = UUID.randomUUID();
        ActivityLog log = ActivityLog.builder()
                .id(id)
                .event(ActivityEventType.REGISTER)
                .build();

        log.onCreate();

        assertThat(log.getId()).isEqualTo(id);
        assertThat(log.getCreatedAt()).isNotNull();
    }
}
