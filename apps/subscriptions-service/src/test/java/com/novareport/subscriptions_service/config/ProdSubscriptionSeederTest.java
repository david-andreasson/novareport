package com.novareport.subscriptions_service.config;

import com.novareport.subscriptions_service.domain.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProdSubscriptionSeederTest {

    @Mock
    private SubscriptionRepository repository;

    private ProdSubscriptionSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new ProdSubscriptionSeeder(repository);
    }

    @Test
    void runDoesNothingWhenSubscriptionAlreadyActive() {
        when(repository.existsByUserIdAndStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                any(), any(), any(), any()
        )).thenReturn(true);

        assertThatCode(() -> seeder.run()).doesNotThrowAnyException();

        verify(repository, never()).save(any());
    }

    @Test
    void runSeedsSubscriptionWhenMissing() {
        when(repository.existsByUserIdAndStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                any(), any(), any(), any()
        )).thenReturn(false);

        assertThatCode(() -> seeder.run()).doesNotThrowAnyException();

        verify(repository).save(any());
    }
}
