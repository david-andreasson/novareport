package com.novareport.reporter_service.service;

import com.novareport.reporter_service.config.ReporterProperties;
import com.novareport.reporter_service.domain.DailyReport;
import com.novareport.reporter_service.domain.DailyReportRepository;
import com.novareport.reporter_service.domain.NewsItem;
import com.novareport.reporter_service.domain.NewsItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class DailyReportServiceTest {

    private DailyReportRepository dailyReportRepository;
    private NewsItemRepository newsItemRepository;
    private ReporterProperties reporterProperties;
    private DailyReportService.FakeSummaryService fakeSummaryService;
    private DailyReportService.AiSummarizerService aiSummarizerService;

    private DailyReportService service;

    @BeforeEach
    void setUp() {
        dailyReportRepository = mock(DailyReportRepository.class);
        newsItemRepository = mock(NewsItemRepository.class);
        reporterProperties = mock(ReporterProperties.class);
        fakeSummaryService = mock(DailyReportService.FakeSummaryService.class);
        aiSummarizerService = mock(DailyReportService.AiSummarizerService.class);
        service = new DailyReportService(dailyReportRepository, newsItemRepository, reporterProperties, fakeSummaryService, aiSummarizerService);
    }

    @Test
    void findLatestDelegatesToRepository() {
        DailyReport report = new DailyReport();
        when(dailyReportRepository.findTop1ByOrderByReportDateDesc()).thenReturn(Optional.of(report));

        Optional<DailyReport> result = service.findLatest();

        assertThat(result).contains(report);
        verify(dailyReportRepository).findTop1ByOrderByReportDateDesc();
    }

    @Test
    void findByDateDelegatesToRepository() {
        LocalDate date = LocalDate.now();
        DailyReport report = new DailyReport();
        when(dailyReportRepository.findByReportDate(date)).thenReturn(Optional.of(report));

        Optional<DailyReport> result = service.findByDate(date);

        assertThat(result).contains(report);
        verify(dailyReportRepository).findByReportDate(date);
    }

    @Test
    void findBetweenDelegatesToRepository() {
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();
        Pageable pageable = Pageable.ofSize(10);
        Page<DailyReport> page = Page.empty(pageable);
        when(dailyReportRepository.findAllByReportDateBetweenOrderByReportDateDesc(from, to, pageable)).thenReturn(page);

        Page<DailyReport> result = service.findBetween(from, to, pageable);

        assertThat(result).isSameAs(page);
        verify(dailyReportRepository).findAllByReportDateBetweenOrderByReportDateDesc(from, to, pageable);
    }

    @Test
    void buildReportUsesFallbackSummaryWhenNoNewsItems() {
        LocalDate date = LocalDate.of(2024, 1, 1);
        when(dailyReportRepository.findByReportDate(date)).thenReturn(Optional.empty());
        when(reporterProperties.dedupWindow()).thenReturn(Duration.ofHours(48));
        when(newsItemRepository.findTop10ByPublishedAtAfterOrderByPublishedAtDesc(any())).thenReturn(List.of());
        when(dailyReportRepository.save(any(DailyReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DailyReport result = service.buildReport(date);

        assertThat(result.getReportDate()).isEqualTo(date);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getSummary()).isEqualTo("No news items available. This may be due to temporary issues reaching external news sources.");
    }

    @Test
    void buildReportUsesFakeSummaryWhenFakeAiEnabled() {
        LocalDate date = LocalDate.of(2024, 1, 2);
        DailyReport existing = new DailyReport();
        existing.setReportDate(date);
        when(dailyReportRepository.findByReportDate(date)).thenReturn(Optional.of(existing));

        when(reporterProperties.dedupWindow()).thenReturn(Duration.ofHours(48));
        NewsItem item = new NewsItem();
        item.setTitle("Title");
        item.setSource("Source");
        item.setPublishedAt(Instant.now());
        when(newsItemRepository.findTop10ByPublishedAtAfterOrderByPublishedAtDesc(any())).thenReturn(List.of(item));

        when(reporterProperties.fakeAi()).thenReturn(true);
        when(fakeSummaryService.buildSummary(eq(date), any())).thenReturn("fake-summary");
        when(dailyReportRepository.save(any(DailyReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DailyReport result = service.buildReport(date);

        assertThat(result.getSummary()).isEqualTo("fake-summary");
    }

    @Test
    void buildReportUsesAiSummaryWhenFakeAiDisabled() {
        LocalDate date = LocalDate.of(2024, 1, 3);
        when(dailyReportRepository.findByReportDate(date)).thenReturn(Optional.empty());

        when(reporterProperties.dedupWindow()).thenReturn(Duration.ofHours(48));
        NewsItem item = new NewsItem();
        item.setTitle("Title");
        item.setSource("Source");
        item.setPublishedAt(Instant.now());
        when(newsItemRepository.findTop10ByPublishedAtAfterOrderByPublishedAtDesc(any())).thenReturn(List.of(item));

        when(reporterProperties.fakeAi()).thenReturn(false);
        when(aiSummarizerService.summarize(eq(date), any())).thenReturn("ai-summary");
        when(dailyReportRepository.save(any(DailyReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DailyReport result = service.buildReport(date);

        assertThat(result.getSummary()).isEqualTo("ai-summary");
    }
}
