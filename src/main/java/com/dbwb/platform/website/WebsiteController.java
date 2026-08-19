package com.dbwb.platform.website;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.publicapi.dto.PublicWebsiteResponse;
import com.dbwb.platform.security.CurrentAccount;
import com.dbwb.platform.website.dto.CreateWebsiteRequest;
import com.dbwb.platform.website.dto.UpdateDraftContentRequest;
import com.dbwb.platform.website.dto.UpdateLayoutVariantRequest;
import com.dbwb.platform.website.dto.UpdateThemeRequest;
import com.dbwb.platform.website.dto.UpdateThemeConfigRequest;
import com.dbwb.platform.website.dto.WebsiteResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/websites")
public class WebsiteController {

    private final WebsiteService websiteService;
    private final CurrentAccount currentAccount;

    public WebsiteController(WebsiteService websiteService, CurrentAccount currentAccount) {
        this.websiteService = websiteService;
        this.currentAccount = currentAccount;
    }

    @PostMapping
    public ApiResponse<WebsiteResponse> create(@Valid @RequestBody CreateWebsiteRequest request) {
        var website = websiteService.create(currentAccount.get(), request);
        return ApiResponse.ok(WebsiteResponse.from(website));
    }

    @GetMapping
    public ApiResponse<List<WebsiteResponse>> listMine() {
        var websites = websiteService.listForOwner(currentAccount.get().accountId());
        return ApiResponse.ok(websites.stream().map(WebsiteResponse::from).toList());
    }

    /** Phase 4 (BR-MGR): every website the caller owns or has ACCEPTED manager access to, with their role/permissions on each. */
    @GetMapping("/accessible")
    public ApiResponse<List<WebsiteResponse>> listAccessible() {
        var accessible = websiteService.listAccessible(currentAccount.get());
        return ApiResponse.ok(accessible.stream()
                .map(a -> WebsiteResponse.from(a.website(), a.role(), a.permissions()))
                .toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<WebsiteResponse> get(@PathVariable UUID id) {
        var access = websiteService.getWithAccess(id, currentAccount.get());
        return ApiResponse.ok(WebsiteResponse.from(access.website(), access.role(), access.permissions()));
    }

    @GetMapping("/{id}/preview")
    public ApiResponse<PublicWebsiteResponse> preview(@PathVariable UUID id) {
        return ApiResponse.ok(websiteService.getPreview(id, currentAccount.get()));
    }

    @PutMapping("/{id}/draft")
    public ApiResponse<WebsiteResponse> saveDraft(@PathVariable UUID id, @Valid @RequestBody UpdateDraftContentRequest request) {
        var website = websiteService.saveDraft(id, currentAccount.get(), request);
        return ApiResponse.ok(WebsiteResponse.from(website), "Draft saved.");
    }

    @PutMapping("/{id}/theme")
    public ApiResponse<WebsiteResponse> updateTheme(@PathVariable UUID id, @RequestBody UpdateThemeRequest request) {
        var website = websiteService.updateTheme(id, currentAccount.get(), request.themeId());
        return ApiResponse.ok(WebsiteResponse.from(website), "Theme updated.");
    }

    @PutMapping("/{id}/theme-config")
    public ApiResponse<WebsiteResponse> updateThemeConfig(@PathVariable UUID id, @RequestBody UpdateThemeConfigRequest request) {
        var website = websiteService.updateThemeConfig(id, currentAccount.get(), request.themeConfig());
        return ApiResponse.ok(WebsiteResponse.from(website), "Theme updated.");
    }

    @PutMapping("/{id}/layout")
    public ApiResponse<WebsiteResponse> updateLayoutVariant(@PathVariable UUID id, @Valid @RequestBody UpdateLayoutVariantRequest request) {
        var website = websiteService.updateLayoutVariant(id, currentAccount.get(), request.layoutVariant());
        return ApiResponse.ok(WebsiteResponse.from(website), "Layout updated.");
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<WebsiteResponse> publish(@PathVariable UUID id) {
        var website = websiteService.publish(id, currentAccount.get());
        return ApiResponse.ok(WebsiteResponse.from(website), "Website published.");
    }

    @PostMapping("/{id}/restore-previous-version")
    public ApiResponse<WebsiteResponse> restorePreviousVersion(@PathVariable UUID id) {
        var website = websiteService.restorePreviousVersion(id, currentAccount.get());
        return ApiResponse.ok(WebsiteResponse.from(website), "Previous published version restored.");
    }
}
