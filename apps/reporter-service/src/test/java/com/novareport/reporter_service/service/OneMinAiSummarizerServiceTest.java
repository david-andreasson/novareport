package com.novareport.reporter_service.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class OneMinAiSummarizerServiceTest {

    @Test
    void summarizeReturnsDefaultWhenNoHeadlines() {
        OneMinAiSummarizerService service = new OneMinAiSummarizerService(WebClient.builder(), "api-key", "gpt-4o-mini");

        LocalDate date = LocalDate.of(2024, 1, 1);
        String resultEmpty = service.summarize(date, List.of());
        String resultNull = service.summarize(date, null);

        assertThat(resultEmpty).contains("No news items available for " + date);
        assertThat(resultNull).contains("No news items available for " + date);
    }

    @Test
    void summarizeUsesAiResponseOnSuccess() {
        WebClient.Builder builder = WebClient.builder()
            .exchangeFunction(request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"aiRecord\":{\"aiRecordDetail\":{\"resultObject\":\"AI SUMMARY\"}}}")
                    .build()
            ));

        OneMinAiSummarizerService service = new OneMinAiSummarizerService(builder, "api-key", "gpt-4o-mini");

        String result = service.summarize(LocalDate.of(2024, 1, 2), List.of("headline"));

        assertThat(result).isEqualTo("AI SUMMARY");
    }

    @Test
    void summarizeFallsBackAfterFailures() {
        HttpHeaders headers = Objects.requireNonNull(new HttpHeaders());

        WebClientResponseException ex = WebClientResponseException.create(
            500,
            "Server Error",
            headers,
            new byte[0],
            StandardCharsets.UTF_8
        );

        WebClient.Builder builder = WebClient.builder()
            .exchangeFunction(request -> Mono.error(ex));

        OneMinAiSummarizerService service = new OneMinAiSummarizerService(builder, "api-key", "gpt-4o-mini");

        LocalDate date = LocalDate.of(2024, 1, 3);
        List<String> headlines = List.of("H1", "H2");

        String result = service.summarize(date, headlines);

        assertThat(result)
            .contains("AI summarization temporarily unavailable")
            .contains("H1")
            .contains("H2");
    }
}
