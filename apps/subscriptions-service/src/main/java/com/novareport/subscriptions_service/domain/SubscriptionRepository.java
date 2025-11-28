package com.novareport.subscriptions_service.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    long countByStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
        SubscriptionStatus status,
        Instant startAt,
        Instant endAt
    );

    @Query("select distinct s.userId from Subscription s where s.status = :status and s.startAt <= :startAt and s.endAt >= :endAt")
    List<UUID> findDistinctUserIdByStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
        @Param("status") SubscriptionStatus status,
        @Param("startAt") Instant startAt,
        @Param("endAt") Instant endAt
    );
}
