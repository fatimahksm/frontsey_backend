package com.dbwb.platform.publicapi;

import com.dbwb.platform.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** No authentication required - see SecurityConfig ("/api/public/**" permitAll). */
@RestController
@RequestMapping("/api/public")
public class PublicWebsiteController {

    private final PublicWebsiteService publicWebsiteService;

    public PublicWebsiteController(PublicWebsiteService publicWebsiteService) {
        this.publicWebsiteService = publicWebsiteService;
    }

    @GetMapping("/websites/{slug}")
    public ApiResponse<PublicWebsiteEnvelope> getBySlug(@PathVariable String slug) {
        return ApiResponse.ok(publicWebsiteService.getBySlug(slug));
    }
}
