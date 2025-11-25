package com.novareport.reporter_service.service;

import com.novareport.reporter_service.config.ReporterProperties;
import com.novareport.reporter_service.domain.NewsItem;
import com.novareport.reporter_service.domain.NewsItemRepository;
import com.novareport.reporter_service.util.LogSanitizer;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import reactor.util.retry.Retry;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
            .onErrorContinue((ex, entry) -> log.warn(
                "Failed to process entry {}: {}",
                LogSanitizer.sanitize(entry),
                LogSanitizer.sanitize(ex.getMessage())
            ))
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
        long maxRetries = 2L;
        long totalAttempts = maxRetries + 1L;
        log.info("Fetching RSS feed {}", LogSanitizer.sanitize(url));

        return webClient
            .get()
            .uri(url)
            .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
            .accept(MediaType.APPLICATION_RSS_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML)
            .retrieve()
            .bodyToMono(String.class)
            .retryWhen(Retry
                .backoff(maxRetries, Duration.ofSeconds(1))
                .filter(ex -> ex instanceof WebClientRequestException)
                .doBeforeRetry(retrySignal -> {
                    Throwable failure = retrySignal.failure();
                    long attempt = retrySignal.totalRetries() + 1L;
                    log.warn("Retrying RSS feed fetch {} (attempt {}/{}) due to: {}",
                        LogSanitizer.sanitize(url),
                        attempt,
                        totalAttempts,
                        failure != null ? LogSanitizer.sanitize(failure.getMessage()) : "unknown error");
                })
                .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
            .filter(xml -> xml != null)
            .flatMapMany(xml -> parseFeed(url, xml))
            .onErrorResume(ex -> {
                log.warn("Failed to fetch RSS feed {} after {} attempts: {}",
                    LogSanitizer.sanitize(url),
                    totalAttempts,
                    LogSanitizer.sanitize(ex.getMessage()));
                return Flux.empty();
            })
            .map(feed -> Tuples.of(url, feed));
    }

    private Flux<SyndFeed> parseFeed(String url, String xml) {
        String raw = xml == null ? "" : xml;
        String sanitized = sanitizeXml(raw);
        String preview = sanitized.length() > 200 ? sanitized.substring(0, 200) + "..." : sanitized;

        if (!sanitized.trim().startsWith("<")) {
            String[] lines = raw.split("\\R", 6);
            int maxLines = Math.min(5, lines.length);
            StringBuilder firstLinesBuilder = new StringBuilder();
            for (int i = 0; i < maxLines; i++) {
                if (i > 0) {
                    firstLinesBuilder.append("\\n");
                }
                firstLinesBuilder.append(lines[i]);
            }
            String firstLines = firstLinesBuilder.toString();
            log.warn(
                "RSS feed {} did not start with XML markup. First {} line(s): {}",
                LogSanitizer.sanitize(url),
                maxLines,
                LogSanitizer.sanitize(firstLines)
            );
            return Flux.empty();
        }

        try (XmlReader reader = new XmlReader(new ByteArrayInputStream(sanitized.getBytes(StandardCharsets.UTF_8)))) {
            SyndFeed feed = new SyndFeedInput().build(reader);
            int entries = feed.getEntries() == null ? 0 : feed.getEntries().size();
            if (entries == 0) {
                log.info("RSS feed {} parsed successfully but contained no entries", url);
            } else {
                log.info("RSS feed {} parsed successfully with {} entries", url, entries);
            }
            return Flux.just(feed);
        } catch (FeedException | IOException | RuntimeException ex) {
            log.warn("Failed to parse RSS feed {}: {}. Preview: {}",
                LogSanitizer.sanitize(url),
                LogSanitizer.sanitize(ex.getMessage()),
                LogSanitizer.sanitize(preview.replaceAll("\\s+", " ")));
            return Flux.empty();
        }
    }

    private String sanitizeXml(String xml) {
        if (xml == null) {
            return "";
        }

        String sanitized = xml.stripLeading();
        if (sanitized.startsWith("\uFEFF")) {
            sanitized = sanitized.substring(1);
        }
        return sanitized;
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
