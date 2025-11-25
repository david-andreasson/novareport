package com.novareport.reporter_service.service;

import com.novareport.reporter_service.util.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@ConditionalOnProperty(name = "reporter.fake-ai", havingValue = "true", matchIfMissing = true)
public class AiSummarizerStub implements DailyReportService.AiSummarizerService {

    private static final Logger log = LoggerFactory.getLogger(AiSummarizerStub.class);

    @Override
    public String summarize(LocalDate date, List<String> headlines) {
        log.info(
            "AI summarizer stub invoked for {} with {} headlines",
            LogSanitizer.sanitize(date),
            headlines.size()
        );
        return "[AI integration pending] Dagens rubriker: " + String.join(", ", headlines);
    }
}
