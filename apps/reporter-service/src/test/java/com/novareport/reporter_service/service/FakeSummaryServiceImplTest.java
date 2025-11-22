package com.novareport.reporter_service.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FakeSummaryServiceImplTest {

    private final FakeSummaryServiceImpl service = new FakeSummaryServiceImpl();

    @Test
    void returnsFallbackTextWhenNoHeadlines() {
        LocalDate date = LocalDate.of(2024, 1, 1);

        String resultNull = service.buildSummary(date, null);
        String resultEmpty = service.buildSummary(date, List.of());

        assertThat(resultNull).isEqualTo("Inga nyheter hittades för " + date + ".");
        assertThat(resultEmpty).isEqualTo("Inga nyheter hittades för " + date + ".");
    }

    @Test
    void formatsHeadlinesIntoBulletList() {
        LocalDate date = LocalDate.of(2024, 1, 2);
        List<String> headlines = List.of("A", "B");

        String result = service.buildSummary(date, headlines);

        assertThat(result)
            .startsWith("Dagens viktigaste nyheter " + date + ":\n- ")
            .contains("- A")
            .contains("- B");
    }
}
