package com.novareport.subscriptions_service.service;

import com.novareport.subscriptions_service.domain.Subscription;
import com.novareport.subscriptions_service.domain.SubscriptionRepository;
import com.novareport.subscriptions_service.domain.SubscriptionStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class SubscriptionService {

    private final SubscriptionRepository repository;
    private final boolean fakeAllActive;

    public SubscriptionService(
        SubscriptionRepository repository,
        @Value("${subs.fake-all-active:false}") boolean fakeAllActive
    ) {
        this.repository = repository;
        this.fakeAllActive = fakeAllActive;
    }

    @Transactional(readOnly = true)
    public boolean hasAccess(UUID userId, Instant now) {
        if (fakeAllActive) {
            return true;
        }
        return repository.existsByUserIdAndStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
            userId,
            SubscriptionStatus.ACTIVE,
            now,
            now
        );
    }

    @Transactional(readOnly = true)
    public Optional<Subscription> findActiveSubscription(UUID userId, Instant now) {
        return repository.findFirstByUserIdAndStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByEndAtDesc(
            userId,
            SubscriptionStatus.ACTIVE,
            now,
            now
        );
    }

    @Transactional
    public Subscription activate(UUID userId, String plan, int durationDays) {
        Instant now = Instant.now();
        Optional<Subscription> existing = findActiveSubscription(userId, now);

        if (existing.isPresent()) {
            Subscription subscription = existing.get();
            Instant base = subscription.getEndAt().isAfter(now) ? subscription.getEndAt() : now;
            subscription.setPlan(plan);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setEndAt(base.plus(Duration.ofDays(durationDays)));
            subscription.setStartAt(subscription.getStartAt() != null ? subscription.getStartAt() : now);
            return repository.save(subscription);
        }

        Subscription created = new Subscription();
        created.setUserId(userId);
        created.setPlan(plan);
        created.setStatus(SubscriptionStatus.ACTIVE);
        created.setStartAt(now);
        created.setEndAt(now.plus(Duration.ofDays(durationDays)));
        return repository.save(created);
    }

    @Transactional
    public Optional<Subscription> cancel(UUID userId) {
        Instant now = Instant.now();
        Optional<Subscription> active = findActiveSubscription(userId, now);
        active.ifPresent(subscription -> {
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscription.setEndAt(now);
            repository.save(subscription);
        });
        return active;
    }
}
