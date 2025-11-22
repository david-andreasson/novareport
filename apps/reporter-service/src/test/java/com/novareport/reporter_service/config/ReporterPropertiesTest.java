package com.novareport.reporter_service.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReporterPropertiesTest {

    @Test
    void rssFeedsReturnsEmptyListWhenNullOrEmpty() {
        ReporterProperties propsNull = new ReporterProperties(null, true, Duration.ofHours(24), false);
        ReporterProperties propsEmpty = new ReporterProperties(List.of(), true, Duration.ofHours(24), false);

        assertThat(propsNull.rssFeeds()).isEmpty();
        assertThat(propsEmpty.rssFeeds()).isEmpty();
    }

    @Test
    void rssFeedsTrimsAndFiltersBlankEntries() {
        ReporterProperties props = new ReporterProperties(List.of("  a  ", " ", "b"), true, Duration.ofHours(24), false);

        assertThat(props.rssFeeds()).containsExactly("a", "b");
    }

    @Test
    void dedupWindowDefaultsTo48HoursWhenNull() {
        ReporterProperties props = new ReporterProperties(List.of("a"), true, null, false);

        assertThat(props.dedupWindow()).isEqualTo(Duration.ofHours(48));
    }

    @Test
    void dedupWindowUsesConfiguredValueWhenPresent() {
        ReporterProperties props = new ReporterProperties(List.of("a"), true, Duration.ofHours(12), false);

        assertThat(props.dedupWindow()).isEqualTo(Duration.ofHours(12));
    }
}
