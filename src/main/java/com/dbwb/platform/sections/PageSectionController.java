package com.dbwb.platform.sections;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.sections.dto.PageSectionRequest;
import com.dbwb.platform.sections.dto.PageSectionResponse;
import com.dbwb.platform.security.CurrentAccount;
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
@RequestMapping("/api/websites/{websiteId}/sections")
public class PageSectionController {

    private final PageSectionService sectionService;
    private final CurrentAccount currentAccount;

    public PageSectionController(PageSectionService sectionService, CurrentAccount currentAccount) {
        this.sectionService = sectionService;
        this.currentAccount = currentAccount;
    }

    @GetMapping
    public ApiResponse<List<PageSectionResponse>> list(@PathVariable UUID websiteId) {
        return ApiResponse.ok(sectionService.list(websiteId, currentAccount.get())
                .stream().map(PageSectionResponse::from).toList());
    }

    @PostMapping
    public ApiResponse<PageSectionResponse> create(@PathVariable UUID websiteId, @Valid @RequestBody PageSectionRequest request) {
        var section = sectionService.create(websiteId, currentAccount.get(), request);
        return ApiResponse.ok(PageSectionResponse.from(section), "Section added.");
    }

    @PutMapping("/{sectionId}")
    public ApiResponse<PageSectionResponse> update(@PathVariable UUID websiteId, @PathVariable UUID sectionId,
                                                    @Valid @RequestBody PageSectionRequest request) {
        var section = sectionService.update(websiteId, sectionId, currentAccount.get(), request);
        return ApiResponse.ok(PageSectionResponse.from(section), "Section updated.");
    }

    @DeleteMapping("/{sectionId}")
    public ApiResponse<Void> delete(@PathVariable UUID websiteId, @PathVariable UUID sectionId) {
        sectionService.delete(websiteId, sectionId, currentAccount.get());
        return ApiResponse.ok(null, "Section deleted.");
    }

    @PutMapping("/reorder")
    public ApiResponse<Void> reorder(@PathVariable UUID websiteId, @RequestBody List<UUID> sectionIds) {
        sectionService.reorder(websiteId, currentAccount.get(), sectionIds);
        return ApiResponse.ok(null, "Sections reordered.");
    }
}
