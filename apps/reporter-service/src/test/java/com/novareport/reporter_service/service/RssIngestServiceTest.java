package com.novareport.reporter_service.service;

import com.novareport.reporter_service.config.ReporterProperties;
import com.novareport.reporter_service.domain.NewsItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class RssIngestServiceTest {

    @Test
    void ingestReturnsEmptyWhenNoFeedsConfigured() {
        WebClient webClient = WebClient.builder().build();
        ReporterProperties properties = new ReporterProperties(List.of(), true, Duration.ofHours(48), false);
        NewsItemRepository newsItemRepository = mock(NewsItemRepository.class);

        RssIngestService service = new RssIngestService(webClient, properties, newsItemRepository);

        RssIngestService.IngestResult result = service.ingest();

        assertThat(result.attempted()).isZero();
        assertThat(result.stored()).isZero();
        verifyNoInteractions(newsItemRepository);
    }

    @Test
    void ingestParsesFeedAndPersistsNewItems() {
        String xml = """
            <?xml version=\"1.0\" encoding=\"UTF-8\"?>
            <rss version=\"2.0\">
              <channel>
                <title>Test Feed</title>
                <item>
                  <title>Item 1</title>
                  <link>https://example.com/1</link>
                  <description>Desc 1</description>
                </item>
                <item>
                  <title>Item 2</title>
                  <link>https://example.com/2</link>
                  <description>Desc 2</description>
                </item>
              </channel>
            </rss>
            """;

        WebClient webClient = WebClient.builder()
            .exchangeFunction(request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                    .body(xml)
                    .build()
            ))
            .build();

        ReporterProperties properties = new ReporterProperties(List.of("https://feed.example.com/rss"), true, Duration.ofHours(48), false);
        NewsItemRepository newsItemRepository = mock(NewsItemRepository.class);

        when(newsItemRepository.findExistingHashes(any())).thenReturn(Set.of());
        when(newsItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RssIngestService service = new RssIngestService(webClient, properties, newsItemRepository);

        RssIngestService.IngestResult result = service.ingest();

        assertThat(result.attempted()).isEqualTo(2L);
        assertThat(result.stored()).isEqualTo(2L);
    }
}
