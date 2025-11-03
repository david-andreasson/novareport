package com.novareport.reporter_service.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.StringJoiner;

@Service
public class FakeSummaryServiceImpl implements DailyReportService.FakeSummaryService {

    @Override
    public String buildSummary(LocalDate date, List<String> headlines) {
        if (headlines == null || headlines.isEmpty()) {
            return "Inga nyheter hittades för " + date + ".";
        }
        StringJoiner joiner = new StringJoiner("\n- ", "Dagens viktigaste nyheter " + date + ":\n- ", "");
        headlines.forEach(joiner::add);
        return joiner.toString();
    }
}
