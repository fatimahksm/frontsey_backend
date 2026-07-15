package com.dbwb.platform.website;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.security.CurrentAccount;
import com.dbwb.platform.website.dto.SeoMetadataRequest;
import com.dbwb.platform.website.dto.SeoMetadataResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/websites/{websiteId}/seo")
public class SeoMetadataController {

    private final SeoMetadataService seoMetadataService;
    private final CurrentAccount currentAccount;

    public SeoMetadataController(SeoMetadataService seoMetadataService, CurrentAccount currentAccount) {
        this.seoMetadataService = seoMetadataService;
        this.currentAccount = currentAccount;
    }

    @GetMapping
    public ApiResponse<SeoMetadataResponse> get(@PathVariable UUID websiteId) {
        return ApiResponse.ok(seoMetadataService.get(websiteId, currentAccount.get())
                .map(SeoMetadataResponse::from)
                .orElseGet(SeoMetadataResponse::empty));
    }

    @PutMapping
    public ApiResponse<SeoMetadataResponse> update(@PathVariable UUID websiteId, @Valid @RequestBody SeoMetadataRequest request) {
        var seo = seoMetadataService.update(websiteId, currentAccount.get(), request);
        return ApiResponse.ok(SeoMetadataResponse.from(seo), "SEO metadata updated.");
    }
}
