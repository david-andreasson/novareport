package com.novareport.reporter_service.service;

import com.novareport.reporter_service.config.ReporterProperties;
import com.novareport.reporter_service.domain.NewsItem;
import com.novareport.reporter_service.domain.NewsItemRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RssIngestService {

    private static final Logger log = LoggerFactory.getLogger(RssIngestService.class);

    private final WebClient webClient;
    private final ReporterProperties properties;
    private final NewsItemRepository newsItemRepository;

    public RssIngestService(WebClient webClient, ReporterProperties properties, NewsItemRepository newsItemRepository) {
        this.webClient = webClient;
        this.properties = properties;
        this.newsItemRepository = newsItemRepository;
    }

    @Transactional
    public IngestResult ingest() {
        List<String> feeds = properties.rssFeeds();
        if (feeds.isEmpty()) {
            log.warn("No RSS feeds configured, skipping ingest");
            return IngestResult.empty();
        }

        List<NewsItem> items = collectNewsItems(feeds);
        long attempted = (long) items.size();
        if (attempted == 0) {
            log.info("RSS ingest completed: no entries processed");
            return new IngestResult(0, 0);
        }

        Map<String, NewsItem> deduped = deduplicateByHash(items);
        long stored = persistNewItems(deduped);
        logIngestSummary(attempted, stored);

        return new IngestResult(attempted, stored);
    }

    private List<NewsItem> collectNewsItems(List<String> feeds) {
        return Flux.fromIterable(feeds)
            .flatMap(this::fetchFeed)
            .flatMap(tuple -> Flux.fromIterable(tuple.getT2().getEntries())
                .map(entry -> toNewsItem(tuple.getT2(), tuple.getT1(), entry)))
            .onErrorContinue((ex, entry) -> log.warn("Failed to process entry {}: {}", entry, ex.getMessage()))
            .collectList()
            .blockOptional()
            .orElse(List.of());
    }

    private Map<String, NewsItem> deduplicateByHash(List<NewsItem> items) {
        if (items.isEmpty()) {
            return Map.of();
        }

        return items.stream()
            .filter(item -> item.getHash() != null)
            .collect(Collectors.toMap(
                NewsItem::getHash,
                item -> item,
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ));
    }

    private long persistNewItems(Map<String, NewsItem> deduped) {
        if (deduped.isEmpty()) {
            return 0L;
        }

        Set<String> existing = newsItemRepository.findExistingHashes(deduped.keySet());
        List<NewsItem> toPersist = deduped.entrySet().stream()
            .filter(entry -> !existing.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .toList();

        return toPersist.isEmpty() ? 0L : (long) newsItemRepository.saveAll(toPersist).size();
    }

    private void logIngestSummary(long attempted, long stored) {
        double storageRatio = attempted == 0 ? 0 : (double) stored / attempted;
        double dedupeRatio = 1 - storageRatio;
        log.info(
            "RSS ingest completed: attempted={}, stored={}, storageRatio={}, dedupeRatio={}",
            attempted,
            stored,
            String.format("%.2f", storageRatio),
            String.format("%.2f", dedupeRatio)
        );
    }

    @SuppressWarnings("null")
    private Flux<Tuple2<String, SyndFeed>> fetchFeed(String url) {
        return webClient
            .get()
            .uri(url)
            .retrieve()
            .bodyToMono(String.class)
            .filter(xml -> xml != null)
            .doOnError(ex -> log.warn("Failed to fetch RSS feed {}: {}", url, ex.getMessage()))
            .flatMapMany(xml -> parseFeed(url, xml))
            .map(feed -> Tuples.of(url, feed));
    }

    private Flux<SyndFeed> parseFeed(String url, String xml) {
        try (XmlReader reader = new XmlReader(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))) {
            SyndFeed feed = new SyndFeedInput().build(reader);
            return Flux.just(feed);
        } catch (Exception ex) {
            log.warn("Failed to parse RSS feed {}: {}", url, ex.getMessage());
            return Flux.empty();
        }
    }

    private NewsItem toNewsItem(SyndFeed feed, String feedUrl, SyndEntry entry) {
        NewsItem item = new NewsItem();
        item.setSource(feed != null && feed.getTitle() != null ? feed.getTitle() : feedUrl);
        item.setUrl(entry.getLink());
        item.setTitle(entry.getTitle());
        item.setSummary(entry.getDescription() != null ? entry.getDescription().getValue() : null);
        item.setPublishedAt(resolvePublishedAt(entry));
        item.setHash(hash(entry.getLink(), entry.getTitle()));
        item.setIngestedAt(Instant.now());
        return item;
    }

    private Instant resolvePublishedAt(SyndEntry entry) {
        if (entry.getPublishedDate() != null) {
            return entry.getPublishedDate().toInstant();
        }
        if (entry.getUpdatedDate() != null) {
            return entry.getUpdatedDate().toInstant();
        }
        return OffsetDateTime.now(ZoneOffset.UTC).toInstant();
    }

    private String hash(String link, String title) {
        String input = (link == null ? "" : link) + "::" + (title == null ? "" : title);
        return DigestUtils.sha256Hex(input);
    }

    public record IngestResult(long attempted, long stored) {
        public static IngestResult empty() {
            return new IngestResult(0, 0);
        }
    }
}
