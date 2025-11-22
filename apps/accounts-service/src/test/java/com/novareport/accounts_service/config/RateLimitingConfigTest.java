package com.novareport.accounts_service.config;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingConfigTest {

    private final RateLimitingConfig config = new RateLimitingConfig();

    @Test
    void rateLimitBucketsReturnsConcurrentMap() {
        Map<String, Bucket> buckets = config.rateLimitBuckets();

        assertThat(buckets).isNotNull();
        assertThat(buckets).isEmpty();
    }

    @Test
    void createBucketProvidesCapacity() {
        Bucket bucket = config.createBucket();

        assertThat(bucket.tryConsume(1)).isTrue();
    }
}
