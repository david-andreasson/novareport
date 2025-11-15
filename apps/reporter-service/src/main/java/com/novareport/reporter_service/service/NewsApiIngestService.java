package com.novareport.reporter_service.service;

import com.novareport.reporter_service.config.NewsApiProperties;
import com.novareport.reporter_service.domain.NewsItem;
import com.novareport.reporter_service.domain.NewsItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NewsApiIngestService {

    private static final Logger log = LoggerFactory.getLogger(NewsApiIngestService.class);

    private final List<NewsApiProvider> providers;
    private final NewsItemRepository newsItemRepository;
    private final NewsApiProperties properties;

    public NewsApiIngestService(
        List<NewsApiProvider> providers,
        NewsItemRepository newsItemRepository,
        NewsApiProperties properties
    ) {
        this.providers = providers;
        this.newsItemRepository = newsItemRepository;
        this.properties = properties;
    }

    @Transactional
    public RssIngestService.IngestResult ingest() {
        if (!properties.enabled()) {
            return RssIngestService.IngestResult.empty();
        }
        if (providers == null || providers.isEmpty()) {
            log.info("News API ingest enabled but no providers are configured, skipping");
            return RssIngestService.IngestResult.empty();
        }

        List<NewsItem> allItems = new ArrayList<>();

        for (NewsApiProvider provider : providers) {
            try {
                List<NewsItem> items = provider.fetchLatestNews();
                if (!items.isEmpty()) {
                    allItems.addAll(items);
                }
                log.info("News API provider {} returned {} items", provider.providerName(), items.size());
            } catch (Exception ex) {
                log.warn("News API provider {} failed: {}", provider.providerName(), ex.getMessage());
            }
        }

        if (allItems.isEmpty()) {
            log.info("News API ingest completed: no entries processed");
            return new RssIngestService.IngestResult(0, 0);
        }

        allItems.sort(Comparator.comparing(NewsItem::getPublishedAt).reversed());

        int maxResults = properties.maxResults();
        if (maxResults > 0 && allItems.size() > maxResults) {
            allItems = allItems.subList(0, maxResults);
        }

        long attempted = allItems.size();

        Map<String, NewsItem> deduped = allItems.stream()
            .filter(item -> item.getHash() != null)
            .collect(Collectors.toMap(
                NewsItem::getHash,
                item -> item,
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ));

        if (deduped.isEmpty()) {
            log.info("News API ingest completed: no entries processed after deduplication");
            return new RssIngestService.IngestResult(attempted, 0);
        }

        Set<String> existing = newsItemRepository.findExistingHashes(deduped.keySet());
        List<NewsItem> toPersist = deduped.entrySet().stream()
            .filter(entry -> !existing.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .toList();

        long stored = toPersist.isEmpty() ? 0L : (long) newsItemRepository.saveAll(toPersist).size();

        double storageRatio = attempted == 0 ? 0 : (double) stored / attempted;
        double dedupeRatio = 1 - storageRatio;
        log.info(
            "News API ingest completed: attempted={}, stored={}, storageRatio={}, dedupeRatio={}",
            attempted,
            stored,
            String.format("%.2f", storageRatio),
            String.format("%.2f", dedupeRatio)
        );

        return new RssIngestService.IngestResult(attempted, stored);
    }
}
