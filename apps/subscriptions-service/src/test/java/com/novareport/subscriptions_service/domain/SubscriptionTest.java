package com.novareport.subscriptions_service.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionTest {

    @Test
    void onCreateSetsCreatedAndUpdatedAt() {
        Subscription subscription = new Subscription();

        subscription.onCreate();

        assertThat(subscription.getCreatedAt()).isNotNull();
        assertThat(subscription.getUpdatedAt()).isNotNull();
        assertThat(subscription.getUpdatedAt()).isEqualTo(subscription.getCreatedAt());
    }

    @Test
    void onUpdateUpdatesUpdatedAtOnly() {
        Subscription subscription = new Subscription();
        subscription.onCreate();
        Instant created = subscription.getCreatedAt();
        Instant firstUpdated = subscription.getUpdatedAt();

        subscription.onUpdate();

        assertThat(subscription.getCreatedAt()).isEqualTo(created);
        assertThat(subscription.getUpdatedAt()).isAfterOrEqualTo(firstUpdated);
    }
}
