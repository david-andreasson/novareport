package com.novareport.reporter_service.service;

import com.novareport.reporter_service.config.NewsApiProperties;
import com.novareport.reporter_service.domain.NewsItem;
import com.novareport.reporter_service.domain.NewsItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class NewsApiIngestServiceTest {

    private NewsItemRepository newsItemRepository;

    @BeforeEach
    void setUp() {
        newsItemRepository = mock(NewsItemRepository.class);
    }

    @Test
    void ingestReturnsEmptyWhenDisabled() {
        NewsApiProperties properties = new NewsApiProperties(false, 20);
        NewsApiIngestService service = new NewsApiIngestService(null, newsItemRepository, properties);

        RssIngestService.IngestResult result = service.ingest();

        assertThat(result.attempted()).isZero();
        assertThat(result.stored()).isZero();
        verifyNoInteractions(newsItemRepository);
    }

    @Test
    void ingestReturnsEmptyWhenNoProvidersConfigured() {
        NewsApiProperties properties = new NewsApiProperties(true, 20);
        NewsApiIngestService service = new NewsApiIngestService(List.of(), newsItemRepository, properties);

        RssIngestService.IngestResult result = service.ingest();

        assertThat(result.attempted()).isZero();
        assertThat(result.stored()).isZero();
    }

    @Test
    void ingestCollectsItemsDeduplicatesAndPersistsNewOnes() {
        NewsApiProvider provider1 = mock(NewsApiProvider.class);
        NewsApiProvider provider2 = mock(NewsApiProvider.class);

        NewsItem item1 = new NewsItem();
        item1.setPublishedAt(Instant.now());
        item1.setHash("h1");
        NewsItem item2 = new NewsItem();
        item2.setPublishedAt(Instant.now());
        item2.setHash("h2");
        NewsItem item3 = new NewsItem();
        item3.setPublishedAt(Instant.now());
        item3.setHash("h2"); // duplicate hash

        when(provider1.fetchLatestNews()).thenReturn(List.of(item1, item2));
        when(provider2.fetchLatestNews()).thenReturn(List.of(item3));

        when(newsItemRepository.findExistingHashes(any())).thenReturn(Set.of("h1"));
        when(newsItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        NewsApiProperties properties = new NewsApiProperties(true, 10);
        NewsApiIngestService service = new NewsApiIngestService(List.of(provider1, provider2), newsItemRepository, properties);

        RssIngestService.IngestResult result = service.ingest();

        assertThat(result.attempted()).isEqualTo(3L);
        assertThat(result.stored()).isEqualTo(1L);
        verify(newsItemRepository).saveAll(any());
    }

    @Test
    void ingestReturnsZeroStoredWhenNoHashesAfterDeduplication() {
        NewsItem item1 = new NewsItem();
        item1.setPublishedAt(Instant.now());
        item1.setHash("h1");

        NewsApiProvider provider = mock(NewsApiProvider.class);
        when(provider.fetchLatestNews()).thenReturn(List.of(item1));

        when(newsItemRepository.findExistingHashes(any())).thenReturn(Set.of("h1"));

        NewsApiProperties properties = new NewsApiProperties(true, 10);
        NewsApiIngestService service = new NewsApiIngestService(List.of(provider), newsItemRepository, properties);

        RssIngestService.IngestResult result = service.ingest();

        assertThat(result.attempted()).isEqualTo(1L);
        assertThat(result.stored()).isZero();
    }
}
