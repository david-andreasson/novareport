package com.novareport.reporter_service.service;

import com.novareport.reporter_service.domain.DailyReportRepository;
import com.novareport.reporter_service.domain.NewsItemRepository;
import org.springframework.stereotype.Service;

@Service
public class ReporterStatusService {

    private final NewsItemRepository newsItemRepository;
    private final DailyReportRepository dailyReportRepository;

    public ReporterStatusService(
        NewsItemRepository newsItemRepository,
        DailyReportRepository dailyReportRepository
    ) {
        this.newsItemRepository = newsItemRepository;
        this.dailyReportRepository = dailyReportRepository;
    }

    public ReporterStatus status() {
        var lastIngested = newsItemRepository.findTop1ByOrderByIngestedAtDesc()
            .map(item -> item.getIngestedAt())
            .orElse(null);
        var lastReport = dailyReportRepository.findTop1ByOrderByReportDateDesc()
            .map(report -> report.getReportDate())
            .orElse(null);
        return new ReporterStatus(lastIngested, lastReport);
    }
}
