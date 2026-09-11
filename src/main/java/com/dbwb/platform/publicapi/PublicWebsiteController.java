package com.dbwb.platform.publicapi;

import com.dbwb.platform.analytics.AnalyticsService;
import com.dbwb.platform.analytics.entity.DeviceType;
import com.dbwb.platform.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
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

    /**
     * The page itself, and nothing that happens once per visitor.
     *
     * Counting the visit used to happen here, which quietly made the whole
     * payload uncacheable: a response that has a side effect cannot be served
     * from a browser or a CDN without losing the count, so every visitor -
     * including the same one on every page of the same site - paid for a full
     * assembly and a full download. The count moved to its own beacon below,
     * and the content is now cacheable for a short window.
     *
     * The window is deliberately short and matched to the server-side cache
     * this already had: an owner who presses Publish wants to see it, and
     * being a minute behind is the most this may cost them.
     */
    @GetMapping("/websites/{slug}")
    public ResponseEntity<ApiResponse<PublicWebsiteEnvelope>> getBySlug(@PathVariable String slug) {
        var lookup = publicWebsiteService.lookupBySlug(slug);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic()
                        .staleWhileRevalidate(Duration.ofSeconds(60)))
                .body(ApiResponse.ok(lookup.envelope()));
    }

    /**
     * One visit, counted.
     *
     * A beacon rather than a side effect of loading the page, so that the page
     * may be cached and the count still be taken. BR-AN-002 says every visit
     * is counted regardless of who is viewing or the site's status, and that
     * still holds - the beacon is sent by every visitor, on every load, cached
     * page or not.
     *
     * It cannot fail a visit: a blocked or failed beacon loses a number, which
     * is the trade this path has always accepted, and never a page.
     */
    @PostMapping("/websites/{slug}/view")
    public ApiResponse<Void> recordPageView(@PathVariable String slug, HttpServletRequest request) {
        publicWebsiteService.findWebsiteIdBySlug(slug).ifPresent(websiteId ->
                analyticsService.recordPageView(websiteId, request.getHeader("Referer"),
                        AnalyticsService.classifyDevice(request.getHeader("User-Agent"))));
        return ApiResponse.ok(null);
    }

    @PostMapping("/websites/{slug}/items/{itemId}/view")
    public ApiResponse<Void> recordItemView(@PathVariable String slug, @PathVariable UUID itemId, HttpServletRequest request) {
        publicWebsiteService.findWebsiteIdBySlug(slug).ifPresent(websiteId -> analyticsService.recordItemView(
                websiteId, itemId, AnalyticsService.classifyDevice(request.getHeader("User-Agent"))));
        return ApiResponse.ok(null);
    }
}
