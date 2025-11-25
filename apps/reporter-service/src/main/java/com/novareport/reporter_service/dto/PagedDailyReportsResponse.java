package com.novareport.reporter_service.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedDailyReportsResponse(
    List<DailyReportResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {

    public PagedDailyReportsResponse {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public static PagedDailyReportsResponse fromPage(Page<DailyReportResponse> page) {
        return new PagedDailyReportsResponse(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }
}
