package com.novareport.reporter_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@ConfigurationProperties(prefix = "reporter")
public record ReporterProperties(
    @DefaultValue("") List<String> rssFeeds,
    @DefaultValue("true") boolean fakeAi,
    @DurationUnit(ChronoUnit.HOURS) @DefaultValue("PT48H") Duration dedupWindowHours
) {
    public List<String> rssFeeds() {
        return this.rssFeeds == null || this.rssFeeds.isEmpty()
            ? List.of()
            : this.rssFeeds.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public Duration dedupWindow() {
        return dedupWindowHours == null ? Duration.ofHours(48) : dedupWindowHours;
    }
}
