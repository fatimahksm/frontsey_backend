package com.dbwb.platform.portfolio;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.portfolio.dto.ExperienceEntryRequest;
import com.dbwb.platform.portfolio.dto.ExperienceEntryResponse;
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
@RequestMapping("/api/websites/{websiteId}/experience")
public class ExperienceEntryController {

    private final ExperienceEntryService service;
    private final CurrentAccount currentAccount;

    public ExperienceEntryController(ExperienceEntryService service, CurrentAccount currentAccount) {
        this.service = service;
        this.currentAccount = currentAccount;
    }

    @GetMapping
    public ApiResponse<List<ExperienceEntryResponse>> list(@PathVariable UUID websiteId) {
        return ApiResponse.ok(service.list(websiteId, currentAccount.get())
                .stream().map(ExperienceEntryResponse::from).toList());
    }

    @PostMapping
    public ApiResponse<ExperienceEntryResponse> create(@PathVariable UUID websiteId,
                                                       @Valid @RequestBody ExperienceEntryRequest request) {
        return ApiResponse.ok(ExperienceEntryResponse.from(service.create(websiteId, currentAccount.get(), request)),
                "Experience added.");
    }

    @PutMapping("/{entryId}")
    public ApiResponse<ExperienceEntryResponse> update(@PathVariable UUID websiteId,
                                                       @PathVariable UUID entryId,
                                                       @Valid @RequestBody ExperienceEntryRequest request) {
        return ApiResponse.ok(ExperienceEntryResponse.from(service.update(websiteId, entryId, currentAccount.get(), request)),
                "Experience updated.");
    }

    @DeleteMapping("/{entryId}")
    public ApiResponse<Void> delete(@PathVariable UUID websiteId, @PathVariable UUID entryId) {
        service.delete(websiteId, entryId, currentAccount.get());
        return ApiResponse.ok(null, "Experience removed.");
    }

    @PutMapping("/reorder")
    public ApiResponse<List<ExperienceEntryResponse>> reorder(@PathVariable UUID websiteId,
                                                              @RequestBody List<UUID> orderedIds) {
        return ApiResponse.ok(service.reorder(websiteId, currentAccount.get(), orderedIds)
                .stream().map(ExperienceEntryResponse::from).toList(), "Order saved.");
    }
}
