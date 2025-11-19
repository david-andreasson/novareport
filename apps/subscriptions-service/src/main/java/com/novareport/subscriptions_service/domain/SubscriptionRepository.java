package com.novareport.subscriptions_service.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findFirstByUserIdAndStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByEndAtDesc(
        UUID userId,
        SubscriptionStatus status,
        Instant startAt,
        Instant endAt
    );

    boolean existsByUserIdAndStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
        UUID userId,
        SubscriptionStatus status,
        Instant startAt,
        Instant endAt
    );

    List<UUID> findDistinctUserIdByStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
        SubscriptionStatus status,
        Instant startAt,
        Instant endAt
    );
}
