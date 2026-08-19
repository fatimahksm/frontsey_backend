package com.dbwb.platform.portfolio;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.portfolio.dto.PortfolioProjectRequest;
import com.dbwb.platform.portfolio.dto.PortfolioProjectResponse;
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
@RequestMapping("/api/websites/{websiteId}/projects")
public class PortfolioProjectController {

    private final PortfolioProjectService service;
    private final CurrentAccount currentAccount;

    public PortfolioProjectController(PortfolioProjectService service, CurrentAccount currentAccount) {
        this.service = service;
        this.currentAccount = currentAccount;
    }

    @GetMapping
    public ApiResponse<List<PortfolioProjectResponse>> list(@PathVariable UUID websiteId) {
        return ApiResponse.ok(service.list(websiteId, currentAccount.get())
                .stream().map(PortfolioProjectResponse::from).toList());
    }

    @PostMapping
    public ApiResponse<PortfolioProjectResponse> create(@PathVariable UUID websiteId,
                                                        @Valid @RequestBody PortfolioProjectRequest request) {
        return ApiResponse.ok(PortfolioProjectResponse.from(service.create(websiteId, currentAccount.get(), request)), "Project added.");
    }

    @PutMapping("/{projectId}")
    public ApiResponse<PortfolioProjectResponse> update(@PathVariable UUID websiteId,
                                                        @PathVariable UUID projectId,
                                                        @Valid @RequestBody PortfolioProjectRequest request) {
        return ApiResponse.ok(PortfolioProjectResponse.from(service.update(websiteId, projectId, currentAccount.get(), request)), "Project updated.");
    }

    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> delete(@PathVariable UUID websiteId, @PathVariable UUID projectId) {
        service.delete(websiteId, projectId, currentAccount.get());
        return ApiResponse.ok(null, "Project removed.");
    }

    @PutMapping("/reorder")
    public ApiResponse<List<PortfolioProjectResponse>> reorder(@PathVariable UUID websiteId,
                                                               @RequestBody List<UUID> orderedIds) {
        return ApiResponse.ok(service.reorder(websiteId, currentAccount.get(), orderedIds)
                .stream().map(PortfolioProjectResponse::from).toList(), "Order saved.");
    }
}
