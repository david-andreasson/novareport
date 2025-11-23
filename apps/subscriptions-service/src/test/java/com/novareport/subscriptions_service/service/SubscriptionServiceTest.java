package com.novareport.subscriptions_service.service;

import com.novareport.subscriptions_service.domain.Subscription;
import com.novareport.subscriptions_service.domain.SubscriptionRepository;
import com.novareport.subscriptions_service.domain.SubscriptionStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository repository;

    private SimpleMeterRegistry meterRegistry;

    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new SubscriptionService(repository, false, meterRegistry);
    }

    @Test
    void hasAccessReturnsTrueWhenFakeAllActive() {
        SubscriptionService fakeService = new SubscriptionService(repository, true, meterRegistry);
        boolean result = fakeService.hasAccess(UUID.randomUUID(), Instant.now());

        assertThat(result).isTrue();
        verifyNoInteractions(repository);
    }

    @Test
    void hasAccessDelegatesToRepositoryWhenNotFake() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        when(repository.existsByUserIdAndStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                userId,
                SubscriptionStatus.ACTIVE,
                now,
                now
        )).thenReturn(true);

        boolean result = service.hasAccess(userId, now);

        assertThat(result).isTrue();
        verify(repository).existsByUserIdAndStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                userId,
                SubscriptionStatus.ACTIVE,
                now,
                now
        );
    }

    @Test
    void findActiveSubscriptionDelegatesToRepository() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        Subscription subscription = new Subscription();
        when(repository.findFirstByUserIdAndStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByEndAtDesc(
                userId,
                SubscriptionStatus.ACTIVE,
                now,
                now
        )).thenReturn(Optional.of(subscription));

        Optional<Subscription> result = service.findActiveSubscription(userId, now);

        assertThat(result).contains(subscription);
        verify(repository).findFirstByUserIdAndStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByEndAtDesc(
                userId,
                SubscriptionStatus.ACTIVE,
                now,
                now
        );
    }

    @Test
    void activateCreatesNewSubscriptionWhenNoneExists() {
        UUID userId = UUID.randomUUID();
        when(repository.findFirstByUserIdAndStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByEndAtDesc(
                any(), any(), any(), any()
        )).thenReturn(Optional.empty());
        when(repository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subscription result = service.activate(userId, "monthly", 30);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getPlan()).isEqualTo("monthly");
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(result.getStartAt()).isNotNull();
        assertThat(result.getEndAt()).isNotNull();
        assertThat(Duration.between(result.getStartAt(), result.getEndAt()).toDays()).isEqualTo(30);
        verify(repository).save(result);
    }

    @Test
    void activateExtendsExistingActiveSubscriptionWhenPresent() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        Subscription existing = new Subscription();
        existing.setUserId(userId);
        existing.setPlan("old-plan");
        existing.setStatus(SubscriptionStatus.ACTIVE);
        existing.setStartAt(now.minus(Duration.ofDays(10)));
        existing.setEndAt(now.plus(Duration.ofDays(5)));

        when(repository.findFirstByUserIdAndStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByEndAtDesc(
                any(), any(), any(), any()
        )).thenReturn(Optional.of(existing));
        when(repository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant previousEndAt = existing.getEndAt();

        Subscription result = service.activate(userId, "new-plan", 10);

        assertThat(result.getPlan()).isEqualTo("new-plan");
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(result.getEndAt()).isAfter(previousEndAt);
        verify(repository).save(existing);
    }

    @Test
    void findActiveUserIdsDelegatesToRepository() {
        Instant now = Instant.now();
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(repository.findDistinctUserIdByStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                SubscriptionStatus.ACTIVE,
                now,
                now
        )).thenReturn(ids);

        List<UUID> result = service.findActiveUserIds(now);

        assertThat(result).isEqualTo(ids);
        verify(repository).findDistinctUserIdByStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                SubscriptionStatus.ACTIVE,
                now,
                now
        );
    }

    @Test
    void cancelMarksActiveSubscriptionAsCancelled() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        Subscription active = new Subscription();
        active.setUserId(userId);
        active.setPlan("plan");
        active.setStatus(SubscriptionStatus.ACTIVE);
        active.setStartAt(now.minus(Duration.ofDays(1)));
        active.setEndAt(now.plus(Duration.ofDays(1)));

        when(repository.findFirstByUserIdAndStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByEndAtDesc(
                any(), any(), any(), any()
        )).thenReturn(Optional.of(active));
        when(repository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Subscription> result = service.cancel(userId);

        assertThat(result).contains(active);
        assertThat(active.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(active.getEndAt()).isNotNull();
        verify(repository).save(active);
    }

    @Test
    void cancelReturnsEmptyWhenNoActiveSubscription() {
        UUID userId = UUID.randomUUID();
        when(repository.findFirstByUserIdAndStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByEndAtDesc(
                any(), any(), any(), any()
        )).thenReturn(Optional.empty());

        Optional<Subscription> result = service.cancel(userId);

        assertThat(result).isEmpty();
        verify(repository, never()).save(any());
    }
}
