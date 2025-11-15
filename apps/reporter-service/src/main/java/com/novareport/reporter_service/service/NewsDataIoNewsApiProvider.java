package com.novareport.reporter_service.service;

import com.novareport.reporter_service.config.NewsDataProperties;
import com.novareport.reporter_service.domain.NewsItem;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NewsDataIoNewsApiProvider implements NewsApiProvider {

    private static final Logger log = LoggerFactory.getLogger(NewsDataIoNewsApiProvider.class);

    private final WebClient webClient;
    private final NewsDataProperties properties;

    public NewsDataIoNewsApiProvider(WebClient webClient, NewsDataProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    @Override
    public String providerName() {
        return "NewsData.io";
    }

    @Override
    public List<NewsItem> fetchLatestNews() {
        if (!properties.enabled()) {
            return List.of();
        }

        String apiKey = properties.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("NewsData.io provider is enabled but API key is not configured; skipping");
            return List.of();
        }

        String baseUrl = properties.resolvedBaseUrl();
        int timeframe = properties.resolvedTimeframeHours();

        String url = baseUrl + "/crypto?apikey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
            + "&language=en"
            + "&timeframe=" + timeframe;

        try {
            NewsDataResponse response = webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(NewsDataResponse.class)
                .block();

            if (response == null || response.results == null || response.results.isEmpty()) {
                log.info("NewsData.io returned no results");
                return List.of();
            }

            List<NewsItem> items = response.results.stream()
                .map(this::toNewsItem)
                .toList();

            log.info("NewsData.io returned {} articles", items.size());
            return items;
        } catch (WebClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            log.warn("Failed to fetch news from NewsData.io: status={} body={}", ex.getStatusCode().value(), body);
            return List.of();
        } catch (Exception ex) {
            log.warn("Failed to fetch news from NewsData.io: {}", ex.getMessage());
            return List.of();
        }
    }

    private NewsItem toNewsItem(NewsDataArticle article) {
        NewsItem item = new NewsItem();

        String source = article.source_id();
        if (source == null || source.isBlank()) {
            source = article.source_url();
        }
        if (source == null || source.isBlank()) {
            source = "NewsData.io";
        }
        item.setSource(source);

        item.setUrl(article.link());
        item.setTitle(article.title());

        String summary = article.description();
        if (summary == null || summary.isBlank()) {
            summary = article.content();
        }
        item.setSummary(summary);

        item.setPublishedAt(parsePubDate(article.pubDate()));
        item.setHash(hash(article.link(), article.title()));
        item.setIngestedAt(Instant.now());

        return item;
    }

    private Instant parsePubDate(String pubDate) {
        if (pubDate == null || pubDate.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(pubDate);
        } catch (Exception ignored) {
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(pubDate, formatter);
            return dateTime.toInstant(ZoneOffset.UTC);
        } catch (Exception ignored) {
        }
        return Instant.now();
    }

    private String hash(String link, String title) {
        String input = (link == null ? "" : link) + "::" + (title == null ? "" : title);
        return DigestUtils.sha256Hex(input);
    }

    private record NewsDataResponse(
        String status,
        Integer totalResults,
        List<NewsDataArticle> results,
        String nextPage
    ) {
    }

    private record NewsDataArticle(
        String article_id,
        String title,
        String link,
        String description,
        String content,
        String pubDate,
        String source_id,
        String source_url,
        String language
    ) {
    }
}
