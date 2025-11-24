package com.novareport.accounts_service.user;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void onCreateInitializesIdAndTimestampsAndMarksNotNew() {
        User user = User.builder()
                .email("user@example.com")
                .passwordHash("hash")
                .build();

        assertThat(user.isNew()).isTrue();

        user.onCreate();

        assertThat(user.getId()).isNotNull();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.isNew()).isFalse();
    }

    @Test
    void onUpdateUpdatesTimestamp() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("hash")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Instant before = user.getUpdatedAt();

        user.onUpdate();

        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void markLoadedSetsIsNewToFalse() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("hash")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Newly built entity is "new" until it has been loaded from the DB or onCreate has run
        assertThat(user.isNew()).isTrue();

        user.markLoaded();

        assertThat(user.isNew()).isFalse();
    }
}
