package com.novareport.reporter_service.controller;

import com.novareport.reporter_service.config.PaginationProperties;
import com.novareport.reporter_service.domain.DailyReport;
import com.novareport.reporter_service.dto.DailyReportResponse;
import com.novareport.reporter_service.dto.PagedDailyReportsResponse;
import com.novareport.reporter_service.service.DailyReportService;
import com.novareport.reporter_service.service.SubscriptionAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class ReportControllerTest {

    private DailyReportService dailyReportService;
    private SubscriptionAccessService subscriptionAccessService;
    private PaginationProperties paginationProperties;

    private ReportController controller;

    @BeforeEach
    void setUp() {
        dailyReportService = mock(DailyReportService.class);
        subscriptionAccessService = mock(SubscriptionAccessService.class);
        paginationProperties = new PaginationProperties();
        paginationProperties.setMaxPageSize(50);
        controller = new ReportController(dailyReportService, subscriptionAccessService, paginationProperties);
    }

    @Test
    void latestReturnsOkWhenReportExists() {
        String auth = "Bearer token";
        DailyReport entity = new DailyReport();
        entity.setId(UUID.randomUUID());
        entity.setReportDate(LocalDate.now());
        entity.setSummary("summary");
        entity.setCreatedAt(Instant.now());

        when(dailyReportService.findLatest()).thenReturn(Optional.of(entity));

        ResponseEntity<DailyReportResponse> response = controller.latest(auth);

        verify(subscriptionAccessService).assertAccess(auth);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().summary()).isEqualTo("summary");
    }

    @Test
    void latestReturnsNotFoundWhenNoReport() {
        String auth = "Bearer token";
        when(dailyReportService.findLatest()).thenReturn(Optional.empty());

        ResponseEntity<DailyReportResponse> response = controller.latest(auth);

        verify(subscriptionAccessService).assertAccess(auth);
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void listReturnsPageOfReports() {
        String auth = "Bearer token";
        LocalDate today = LocalDate.now();
        DailyReport entity = new DailyReport();
        entity.setId(UUID.randomUUID());
        entity.setReportDate(today);
        entity.setSummary("summary");
        entity.setCreatedAt(Instant.now());

        Page<DailyReport> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1);
        when(dailyReportService.findBetween(any(LocalDate.class), any(LocalDate.class), eq(PageRequest.of(0, 10)))).thenReturn(page);

        ResponseEntity<PagedDailyReportsResponse> response = controller.list(auth, null, null, 0, 10);

        verify(subscriptionAccessService).assertAccess(auth);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(1);
        assertThat(response.getBody().content().get(0).summary()).isEqualTo("summary");
        assertThat(response.getBody().page()).isEqualTo(0);
        assertThat(response.getBody().size()).isEqualTo(10);
        assertThat(response.getBody().totalElements()).isEqualTo(1);
        assertThat(response.getBody().totalPages()).isEqualTo(1);
    }

    @Test
    void listThrowsWhenPageNegative() {
        String auth = "Bearer token";

        assertThatThrownBy(() -> controller.list(auth, null, null, -1, 10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("page must be >= 0");
    }

    @Test
    void listThrowsWhenSizeOutOfRange() {
        String auth = "Bearer token";

        assertThatThrownBy(() -> controller.list(auth, null, null, 0, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("size must be between 1 and ");

        assertThatThrownBy(() -> controller.list(auth, null, null, 0, 51))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("size must be between 1 and ");
    }

    @Test
    void listThrowsWhenFromAfterTo() {
        String auth = "Bearer token";
        LocalDate from = LocalDate.of(2024, 2, 1);
        LocalDate to = LocalDate.of(2024, 1, 1);

        assertThatThrownBy(() -> controller.list(auth, from, to, 0, 10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("from must be on or before to");
    }
}
