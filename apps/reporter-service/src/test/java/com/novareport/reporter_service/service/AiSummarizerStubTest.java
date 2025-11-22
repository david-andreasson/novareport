package com.novareport.reporter_service.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiSummarizerStubTest {

    private final AiSummarizerStub stub = new AiSummarizerStub();

    @Test
    void summarizeReturnsStubMessageWithHeadlines() {
        LocalDate date = LocalDate.of(2024, 1, 3);
        List<String> headlines = List.of("H1", "H2");

        String result = stub.summarize(date, headlines);

        assertThat(result)
            .contains("[AI integration pending]")
            .contains("H1")
            .contains("H2");
    }
}
