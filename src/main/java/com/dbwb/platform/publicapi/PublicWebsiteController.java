package com.dbwb.platform.publicapi;

import com.dbwb.platform.analytics.AnalyticsService;
import com.dbwb.platform.analytics.entity.DeviceType;
import com.dbwb.platform.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** No authentication required - see SecurityConfig ("/api/public/**" permitAll). */
@RestController
@RequestMapping("/api/public")
public class PublicWebsiteController {

    private final PublicWebsiteService publicWebsiteService;
    private final AnalyticsService analyticsService;

    public PublicWebsiteController(PublicWebsiteService publicWebsiteService, AnalyticsService analyticsService) {
        this.publicWebsiteService = publicWebsiteService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/websites/{slug}")
    public ApiResponse<PublicWebsiteEnvelope> getBySlug(@PathVariable String slug, HttpServletRequest request) {
        var envelope = publicWebsiteService.getBySlug(slug);

        // BR-AN-002: every visit is counted, regardless of who's viewing or the site's status.
        publicWebsiteService.findWebsiteIdBySlug(slug).ifPresent(websiteId -> analyticsService.recordPageView(
                websiteId, request.getHeader("Referer"), AnalyticsService.classifyDevice(request.getHeader("User-Agent"))));

        return ApiResponse.ok(envelope);
    }

    @PostMapping("/websites/{slug}/items/{itemId}/view")
    public ApiResponse<Void> recordItemView(@PathVariable String slug, @PathVariable UUID itemId, HttpServletRequest request) {
        publicWebsiteService.findWebsiteIdBySlug(slug).ifPresent(websiteId -> analyticsService.recordItemView(
                websiteId, itemId, AnalyticsService.classifyDevice(request.getHeader("User-Agent"))));
        return ApiResponse.ok(null);
    }
}
