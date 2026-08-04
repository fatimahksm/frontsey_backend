package com.dbwb.platform.analytics;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.analytics.dto.AnalyticsSummaryResponse;
import com.dbwb.platform.security.CurrentAccount;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequestMapping("/api/websites/{websiteId}/analytics")
public class AnalyticsController {

    private static final int DEFAULT_RANGE_DAYS = 30;

    private final AnalyticsService analyticsService;
    private final CurrentAccount currentAccount;

    public AnalyticsController(AnalyticsService analyticsService, CurrentAccount currentAccount) {
        this.analyticsService = analyticsService;
        this.currentAccount = currentAccount;
    }

    @GetMapping("/summary")
    public ApiResponse<AnalyticsSummaryResponse> summary(@PathVariable UUID websiteId,
                                                           @RequestParam(required = false) Instant from,
                                                           @RequestParam(required = false) Instant to) {
        Instant effectiveTo = to != null ? to : Instant.now();
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(DEFAULT_RANGE_DAYS, ChronoUnit.DAYS);
        return ApiResponse.ok(analyticsService.getSummary(websiteId, currentAccount.get(), effectiveFrom, effectiveTo));
    }

    /** BR-AN-003: exported as CSV (see AnalyticsService for why, pending TBD-009). */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@PathVariable UUID websiteId,
                                          @RequestParam(required = false) Instant from,
                                          @RequestParam(required = false) Instant to) {
        Instant effectiveTo = to != null ? to : Instant.now();
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(DEFAULT_RANGE_DAYS, ChronoUnit.DAYS);
        String csv = analyticsService.exportSummaryCsv(websiteId, currentAccount.get(), effectiveFrom, effectiveTo);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"analytics-report.csv\"")
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
