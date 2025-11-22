package com.novareport.reporter_service.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NewsItemTest {

    @Test
    void onPersistSetsIngestedAtWhenNull() {
        NewsItem item = new NewsItem();

        assertThat(item.getIngestedAt()).isNull();

        item.onPersist();

        assertThat(item.getIngestedAt()).isNotNull();
    }

    @Test
    void onPersistDoesNotOverrideExistingIngestedAt() {
        NewsItem item = new NewsItem();
        Instant ts = Instant.now().minusSeconds(30);
        item.setIngestedAt(ts);

        item.onPersist();

        assertThat(item.getIngestedAt()).isEqualTo(ts);
    }
}
