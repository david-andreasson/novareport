package com.novareport.reporter_service.controller;

import com.novareport.reporter_service.dto.DailyReportResponse;
import com.novareport.reporter_service.dto.PagedDailyReportsResponse;
import com.novareport.reporter_service.service.DailyReportService;
import com.novareport.reporter_service.service.SubscriptionAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports")
public class ReportController {

    private final DailyReportService dailyReportService;
    private final SubscriptionAccessService subscriptionAccessService;

    public ReportController(
        DailyReportService dailyReportService,
        SubscriptionAccessService subscriptionAccessService
    ) {
        this.dailyReportService = dailyReportService;
        this.subscriptionAccessService = subscriptionAccessService;
    }

    @GetMapping("/latest")
    @Operation(summary = "Get the latest daily report")
    public ResponseEntity<DailyReportResponse> latest(
        @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        subscriptionAccessService.assertAccess(authorization);
        return dailyReportService.findLatest()
            .map(DailyReportResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get paginated reports between dates")
    public ResponseEntity<PagedDailyReportsResponse> list(
        @RequestHeader(name = "Authorization", required = false) String authorization,
        @RequestParam(name = "from", required = false) LocalDate from,
        @RequestParam(name = "to", required = false) LocalDate to,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        subscriptionAccessService.assertAccess(authorization);
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > 50) {
            throw new IllegalArgumentException("size must be between 1 and 50");
        }
        Pageable pageable = PageRequest.of(page, size);
        LocalDate effectiveTo = Optional.ofNullable(to).orElse(LocalDate.now());
        LocalDate effectiveFrom = Optional.ofNullable(from).orElse(effectiveTo.minusDays(30));
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException("from must be on or before to");
        }

        Page<DailyReportResponse> result = dailyReportService.findBetween(effectiveFrom, effectiveTo, pageable)
            .map(DailyReportResponse::fromEntity);
        return ResponseEntity.ok(PagedDailyReportsResponse.fromPage(result));
    }
}
