package com.novareport.reporter_service.dto;

import com.novareport.reporter_service.service.RssIngestService;

public record IngestResponse(long attempted, long stored) {
    public static IngestResponse fromResult(RssIngestService.IngestResult result) {
        return new IngestResponse(result.attempted(), result.stored());
    }
}
