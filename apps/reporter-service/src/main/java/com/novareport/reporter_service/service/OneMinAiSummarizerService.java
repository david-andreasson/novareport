package com.novareport.reporter_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AI summarizer implementation using 1min.ai API.
 * Documentation: https://docs.1min.ai/docs/api/ai-feature-api
 * 
 * This service is only active when reporter.fake-ai=false
 */
@Service
@ConditionalOnProperty(name = "reporter.fake-ai", havingValue = "false")
public class OneMinAiSummarizerService implements DailyReportService.AiSummarizerService {

    private static final Logger log = LoggerFactory.getLogger(OneMinAiSummarizerService.class);
    private static final String API_URL = "https://api.1min.ai/api/features";

    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public OneMinAiSummarizerService(
            WebClient.Builder webClientBuilder,
            @Value("${onemin.api-key}") String apiKey,
            @Value("${onemin.model:gpt-4o-mini}") String model
    ) {
        this.webClient = webClientBuilder.baseUrl(API_URL).build();
        this.apiKey = apiKey;
        this.model = model;
        log.info("OneMinAiSummarizerService initialized with model: {}", model);
    }

    @Override
    public String summarize(LocalDate date, List<String> headlines) {
        if (headlines == null || headlines.isEmpty()) {
            log.warn("No headlines provided for summarization on {}", date);
            return "No news items available for " + date + ".";
        }

        log.info("Generating AI summary for {} with {} headlines using model {}", date, headlines.size(), model);

        String prompt = buildPrompt(date, headlines);

        int maxAttempts = 3;
        long delayMillis = 1000L;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (attempt > 1) {
                    log.info("Retrying 1min.ai call (attempt {}/{})", attempt, maxAttempts);
                }

                String summary = callOneMinAi(prompt);
                log.info("Successfully generated AI summary with {} characters on attempt {}", summary.length(), attempt);
                return summary;

            } catch (WebClientResponseException e) {
                log.error("1min.ai API error on attempt {}/{}: status={}, body={}",
                        attempt, maxAttempts, e.getStatusCode(), e.getResponseBodyAsString(), e);

            } catch (WebClientRequestException e) {
                log.error("1min.ai request error on attempt {}/{}: {}", attempt, maxAttempts, e.getMessage(), e);

            } catch (Exception e) {
                log.error("Unexpected error while generating AI summary on attempt {}/{}", attempt, maxAttempts, e);
            }

            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                delayMillis *= 2;
            }
        }

        log.warn("Falling back to non-AI summary after {} failed attempts to reach 1min.ai", maxAttempts);
        return buildFallbackSummary(date, headlines, "Service unavailable after multiple attempts");
    }

    @SuppressWarnings("unchecked")
    private String callOneMinAi(String prompt) {
        // Combine system prompt and user prompt into one
        String fullPrompt = buildSystemPrompt() + "\n\n" + prompt;
        
        Map<String, Object> promptObject = Map.of(
                "prompt", fullPrompt,
                "isMixed", false,
                "webSearch", false,
                "maxWord", 800
        );
        
        Map<String, Object> request = Map.of(
                "type", "CHAT_WITH_AI",
                "model", model,
                "promptObject", promptObject
        );

        log.debug("Calling 1min.ai API with model: {}", model);

        Map<String, Object> response = webClient.post()
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .header("API-KEY", apiKey)
                .bodyValue(Objects.requireNonNull(request))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return extractSummary(response);
    }

    private String buildSystemPrompt() {
        return """
                You are a professional cryptocurrency news analyst and report writer.
                Your task is to analyze cryptocurrency news and create comprehensive, well-structured daily reports.
                
                Guidelines:
                - Write in a professional, neutral, and informative tone
                - Focus on the most significant developments and trends
                - Organize information logically with clear sections
                - Provide context and explain technical concepts when necessary
                - Highlight market implications and potential impacts
                - Do NOT provide financial advice or investment recommendations
                - Stick to factual reporting based on the provided news items
                - Write in English
                
                Structure your report with:
                1. Executive Summary (2-3 sentences overview)
                2. Key Developments (main news items with analysis)
                3. Market Trends (patterns and themes)
                4. Outlook (what to watch for)
                """;
    }

    private String buildPrompt(LocalDate date, List<String> headlines) {
        StringBuilder sb = new StringBuilder();
        sb.append("Create a comprehensive cryptocurrency market report for ").append(date).append(".\n\n");
        sb.append("Analyze and synthesize the following news items into a cohesive report:\n\n");

        for (int i = 0; i < headlines.size(); i++) {
            sb.append(i + 1).append(". ").append(headlines.get(i)).append("\n");
        }

        sb.append("\n");
        sb.append("Requirements:\n");
        sb.append("- Provide a comprehensive analysis that connects related news items\n");
        sb.append("- Identify and explain key trends and patterns\n");
        sb.append("- Highlight the most significant developments\n");
        sb.append("- Include relevant context and implications\n");
        sb.append("- Write 4-6 well-structured paragraphs (approximately 500-800 words)\n");
        sb.append("- Use clear headings for different sections\n");
        sb.append("- Maintain a professional, analytical tone\n");

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String extractSummary(Map<String, Object> response) {
        if (response == null) {
            throw new RuntimeException("Received null response from 1min.ai API");
        }

        Map<String, Object> aiRecord = (Map<String, Object>) response.get("aiRecord");
        if (aiRecord == null) {
            throw new RuntimeException("No aiRecord in API response");
        }

        Map<String, Object> aiRecordDetail = (Map<String, Object>) aiRecord.get("aiRecordDetail");
        if (aiRecordDetail == null) {
            throw new RuntimeException("No aiRecordDetail in API response");
        }

        Object resultObject = aiRecordDetail.get("resultObject");
        if (resultObject == null) {
            throw new RuntimeException("No resultObject in API response");
        }

        // resultObject can be either a String or a List
        String content;
        if (resultObject instanceof String) {
            content = (String) resultObject;
        } else if (resultObject instanceof List) {
            List<?> resultList = (List<?>) resultObject;
            if (resultList.isEmpty()) {
                throw new RuntimeException("Empty resultObject list in API response");
            }
            content = resultList.get(0).toString();
        } else {
            throw new RuntimeException("Unexpected resultObject type: " + resultObject.getClass());
        }

        if (content == null || content.isBlank()) {
            throw new RuntimeException("Empty content in API response");
        }

        return content.trim();
    }

    private String buildFallbackSummary(LocalDate date, List<String> headlines, String errorMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Cryptocurrency Market Report - ").append(date).append("\n\n");
        sb.append("*Note: AI summarization temporarily unavailable due to a temporary connectivity issue.*\n\n");
        sb.append("## Key Headlines\n\n");

        for (String headline : headlines) {
            sb.append("- ").append(headline).append("\n");
        }

        sb.append("\n*Full AI-powered analysis will be available once the service is restored.*");
        return sb.toString();
    }
}
