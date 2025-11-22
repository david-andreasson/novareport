package com.novareport.reporter_service.service;

import com.novareport.reporter_service.config.NewsDataProperties;
import com.novareport.reporter_service.domain.NewsItem;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NewsDataIoNewsApiProviderTest {

    @Test
    void fetchLatestNewsReturnsEmptyWhenDisabledOrMissingApiKey() {
        WebClient webClient = WebClient.builder().build();

        NewsDataProperties disabled = new NewsDataProperties(false, "https://newsdata.io/api/1", null, 24);
        NewsDataIoNewsApiProvider providerDisabled = new NewsDataIoNewsApiProvider(webClient, disabled);
        assertThat(providerDisabled.fetchLatestNews()).isEmpty();

        NewsDataProperties enabledNoKey = new NewsDataProperties(true, "https://newsdata.io/api/1", "", 24);
        NewsDataIoNewsApiProvider providerNoKey = new NewsDataIoNewsApiProvider(webClient, enabledNoKey);
        assertThat(providerNoKey.fetchLatestNews()).isEmpty();
    }

    @Test
    void fetchLatestNewsParsesResponse() {
        String body = "{\"status\":\"success\",\"totalResults\":1,\"results\":[{\"article_id\":\"1\",\"title\":\"Title\",\"link\":\"https://example.com/1\",\"description\":\"Desc\",\"content\":\"Content\",\"pubDate\":\"2024-01-01T00:00:00Z\",\"source_id\":\"src\",\"source_url\":\"https://src\",\"language\":\"en\"}],\"nextPage\":null}";

        WebClient webClient = WebClient.builder()
            .exchangeFunction(request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .build()
            ))
            .build();

        NewsDataProperties props = new NewsDataProperties(true, "https://news.example.com", "api-key-123", 24);
        NewsDataIoNewsApiProvider provider = new NewsDataIoNewsApiProvider(webClient, props);

        List<NewsItem> items = provider.fetchLatestNews();

        assertThat(items).hasSize(1);
        NewsItem item = items.get(0);
        assertThat(item.getTitle()).isEqualTo("Title");
        assertThat(item.getUrl()).isEqualTo("https://example.com/1");
        assertThat(item.getSource()).isNotBlank();
        assertThat(item.getHash()).isNotBlank();
    }

    @Test
    void fetchLatestNewsReturnsEmptyOnHttpError() {
        WebClientResponseException ex = WebClientResponseException.create(
            500,
            "Server Error",
            new HttpHeaders(),
            new byte[0],
            StandardCharsets.UTF_8
        );

        WebClient webClient = WebClient.builder()
            .exchangeFunction(request -> Mono.error(ex))
            .build();

        NewsDataProperties props = new NewsDataProperties(true, "https://news.example.com", "api-key-123", 24);
        NewsDataIoNewsApiProvider provider = new NewsDataIoNewsApiProvider(webClient, props);

        List<NewsItem> items = provider.fetchLatestNews();

        assertThat(items).isEmpty();
    }
}
