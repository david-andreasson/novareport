package com.novareport.reporter_service.service;

import com.novareport.reporter_service.config.ReporterProperties;
import com.novareport.reporter_service.domain.DailyReport;
import com.novareport.reporter_service.domain.DailyReportRepository;
import com.novareport.reporter_service.domain.NewsItem;
import com.novareport.reporter_service.domain.NewsItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DailyReportService {

    private static final Logger log = LoggerFactory.getLogger(DailyReportService.class);

    private final DailyReportRepository dailyReportRepository;
    private final NewsItemRepository newsItemRepository;
    private final ReporterProperties properties;
    private final FakeSummaryService fakeSummaryService;
    private final AiSummarizerService aiSummarizerService;

    public DailyReportService(
        DailyReportRepository dailyReportRepository,
        NewsItemRepository newsItemRepository,
        ReporterProperties properties,
        FakeSummaryService fakeSummaryService,
        AiSummarizerService aiSummarizerService
    ) {
        this.dailyReportRepository = dailyReportRepository;
        this.newsItemRepository = newsItemRepository;
        this.properties = properties;
        this.fakeSummaryService = fakeSummaryService;
        this.aiSummarizerService = aiSummarizerService;
    }

    @Transactional(readOnly = true)
    public Optional<DailyReport> findLatest() {
        return dailyReportRepository.findTop1ByOrderByReportDateDesc();
    }

    @Transactional(readOnly = true)
    public Optional<DailyReport> findByDate(LocalDate date) {
        return dailyReportRepository.findByReportDate(date);
    }

    @Transactional(readOnly = true)
    public Page<DailyReport> findBetween(LocalDate from, LocalDate to, Pageable pageable) {
        return dailyReportRepository.findAllByReportDateBetweenOrderByReportDateDesc(from, to, pageable);
    }

    @Transactional
    public DailyReport buildReport(LocalDate reportDate) {
        DailyReport report = dailyReportRepository.findByReportDate(reportDate)
            .orElseGet(() -> {
                DailyReport created = new DailyReport();
                created.setReportDate(reportDate);
                return created;
            });

        String summary = generateSummary(reportDate);
        report.setSummary(summary);
        report.setCreatedAt(Instant.now());
        DailyReport saved = dailyReportRepository.save(report);
        log.info("Built report for {} with summary length {} chars", reportDate, summary.length());
        return saved;
    }

    private String generateSummary(LocalDate reportDate) {
        Duration window = properties.dedupWindow();
        Instant threshold = Instant.now().minus(window);
        List<NewsItem> recentItems = newsItemRepository.findTop10ByPublishedAtAfterOrderByPublishedAtDesc(threshold);
        if (recentItems.isEmpty()) {
            log.warn("No news items found within {} hours for report {}", window.toHours(), reportDate);
            return "No news items available. This may be due to temporary issues reaching external news sources.";
        }

        List<String> headlines = recentItems.stream()
            .sorted(Comparator.comparing(NewsItem::getPublishedAt).reversed())
            .map(item -> String.format("%s (%s)", item.getTitle(), item.getSource()))
            .collect(Collectors.toList());

        if (properties.fakeAi()) {
            return fakeSummaryService.buildSummary(reportDate, headlines);
        }
        return aiSummarizerService.summarize(reportDate, headlines);
    }

    public interface FakeSummaryService {
        String buildSummary(LocalDate date, List<String> headlines);
    }

    public interface AiSummarizerService {
        String summarize(LocalDate date, List<String> headlines);
    }
}
