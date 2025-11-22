package com.novareport.reporter_service.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NewsDataPropertiesTest {

    @Test
    void resolvedBaseUrlDefaultsWhenBlankOrNull() {
        NewsDataProperties propsNull = new NewsDataProperties(true, null, "key", 24);
        NewsDataProperties propsBlank = new NewsDataProperties(true, "  ", "key", 24);

        assertThat(propsNull.resolvedBaseUrl()).isEqualTo("https://newsdata.io/api/1");
        assertThat(propsBlank.resolvedBaseUrl()).isEqualTo("https://newsdata.io/api/1");
    }

    @Test
    void resolvedBaseUrlUsesConfiguredValue() {
        NewsDataProperties props = new NewsDataProperties(true, "https://example.com", "key", 24);

        assertThat(props.resolvedBaseUrl()).isEqualTo("https://example.com");
    }

    @Test
    void resolvedTimeframeHoursIsClampedBetween1And48() {
        NewsDataProperties tooLow = new NewsDataProperties(true, null, "key", 0);
        NewsDataProperties tooHigh = new NewsDataProperties(true, null, "key", 72);
        NewsDataProperties ok = new NewsDataProperties(true, null, "key", 24);

        assertThat(tooLow.resolvedTimeframeHours()).isEqualTo(1);
        assertThat(tooHigh.resolvedTimeframeHours()).isEqualTo(48);
        assertThat(ok.resolvedTimeframeHours()).isEqualTo(24);
    }
}
