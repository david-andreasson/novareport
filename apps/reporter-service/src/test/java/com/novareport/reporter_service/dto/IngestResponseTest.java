package com.novareport.reporter_service.dto;

import com.novareport.reporter_service.service.RssIngestService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IngestResponseTest {

    @Test
    void fromResultCopiesValues() {
        RssIngestService.IngestResult result = new RssIngestService.IngestResult(5L, 3L);

        IngestResponse response = IngestResponse.fromResult(result);

        assertThat(response.attempted()).isEqualTo(5L);
        assertThat(response.stored()).isEqualTo(3L);
    }
}
