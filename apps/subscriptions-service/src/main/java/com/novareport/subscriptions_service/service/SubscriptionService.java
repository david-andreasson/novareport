package com.novareport.subscriptions_service.service;

import com.novareport.subscriptions_service.domain.Subscription;
import com.novareport.subscriptions_service.domain.SubscriptionRepository;
import com.novareport.subscriptions_service.domain.SubscriptionStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
    private final MeterRegistry meterRegistry;

    public SubscriptionService(
        SubscriptionRepository repository,
        @Value("${subs.fake-all-active:false}") boolean fakeAllActive,
        MeterRegistry meterRegistry
    ) {
        this.repository = repository;
        this.fakeAllActive = fakeAllActive;
        this.meterRegistry = meterRegistry;
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
        Timer.Sample sample = Timer.start(meterRegistry);
        String kind = "create";
        String planTag = plan != null ? plan : "unknown";
        String outcome = "error";
        try {
            Instant now = Instant.now();
            Optional<Subscription> existing = findActiveSubscription(userId, now);

            if (existing.isPresent()) {
                kind = "extend";
                Subscription subscription = existing.get();
                Instant base = subscription.getEndAt().isAfter(now) ? subscription.getEndAt() : now;
                subscription.setPlan(plan);
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscription.setEndAt(base.plus(Duration.ofDays(durationDays)));
                Subscription saved = repository.save(subscription);
                outcome = "success";
                return saved;
            }

            kind = "create";
            Subscription created = new Subscription();
            created.setUserId(userId);
            created.setPlan(plan);
            created.setStatus(SubscriptionStatus.ACTIVE);
            created.setStartAt(now);
            created.setEndAt(now.plus(Duration.ofDays(durationDays)));
            Subscription saved = repository.save(created);
            outcome = "success";
            return saved;
        } catch (RuntimeException ex) {
            outcome = "error";
            throw ex;
        } finally {
            meterRegistry.counter("nova_subscriptions_activated_total",
                    "kind", kind,
                    "plan", planTag,
                    "outcome", outcome
            ).increment();
            sample.stop(
                    Timer.builder("nova_subscriptions_activation_latency_seconds")
                            .tag("kind", kind)
                            .tag("plan", planTag)
                            .tag("outcome", outcome)
                            .register(meterRegistry)
            );
        }
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
