package com.novareport.subscriptions_service.config;

import com.novareport.subscriptions_service.domain.Subscription;
import com.novareport.subscriptions_service.domain.SubscriptionRepository;
import com.novareport.subscriptions_service.domain.SubscriptionStatus;
import com.novareport.subscriptions_service.util.LogSanitizer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProdSubscriptionSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProdSubscriptionSeeder.class);

    private static final UUID ACTIVE_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String ACTIVE_PLAN = "monthly";

    private final SubscriptionRepository repository;

    @Override
    public void run(String... args) {
        Instant now = Instant.now();
        boolean alreadyActive = repository.existsByUserIdAndStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
            ACTIVE_USER_ID,
            SubscriptionStatus.ACTIVE,
            now,
            now
        );
        if (alreadyActive) {
            log.debug("Prod subscription already present for user {}", LogSanitizer.sanitize(ACTIVE_USER_ID));
            return;
        }

        log.info("Seeding active subscription for prod test user {}", LogSanitizer.sanitize(ACTIVE_USER_ID));
        Subscription subscription = new Subscription();
        subscription.setUserId(ACTIVE_USER_ID);
        subscription.setPlan(ACTIVE_PLAN);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartAt(now.minus(1, ChronoUnit.DAYS));
        subscription.setEndAt(now.plus(30, ChronoUnit.DAYS));
        repository.save(subscription);
    }
}
