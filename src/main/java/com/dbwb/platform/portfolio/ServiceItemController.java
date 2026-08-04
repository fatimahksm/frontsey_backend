package com.dbwb.platform.portfolio;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.portfolio.dto.ServiceItemRequest;
import com.dbwb.platform.portfolio.dto.ServiceItemResponse;
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
@RequestMapping("/api/websites/{websiteId}/services")
public class ServiceItemController {

    private final ServiceItemService serviceItemService;
    private final CurrentAccount currentAccount;

    public ServiceItemController(ServiceItemService serviceItemService, CurrentAccount currentAccount) {
        this.serviceItemService = serviceItemService;
        this.currentAccount = currentAccount;
    }

    @GetMapping
    public ApiResponse<List<ServiceItemResponse>> list(@PathVariable UUID websiteId) {
        return ApiResponse.ok(serviceItemService.list(websiteId, currentAccount.get())
                .stream().map(ServiceItemResponse::from).toList());
    }

    @PostMapping
    public ApiResponse<ServiceItemResponse> create(@PathVariable UUID websiteId, @Valid @RequestBody ServiceItemRequest request) {
        var service = serviceItemService.create(websiteId, currentAccount.get(), request);
        return ApiResponse.ok(ServiceItemResponse.from(service), "Service added.");
    }

    @PutMapping("/{serviceId}")
    public ApiResponse<ServiceItemResponse> update(@PathVariable UUID websiteId, @PathVariable UUID serviceId,
                                                    @Valid @RequestBody ServiceItemRequest request) {
        var service = serviceItemService.update(websiteId, serviceId, currentAccount.get(), request);
        return ApiResponse.ok(ServiceItemResponse.from(service), "Service updated.");
    }

    @DeleteMapping("/{serviceId}")
    public ApiResponse<Void> delete(@PathVariable UUID websiteId, @PathVariable UUID serviceId) {
        serviceItemService.delete(websiteId, serviceId, currentAccount.get());
        return ApiResponse.ok(null, "Service deleted.");
    }

    @PutMapping("/reorder")
    public ApiResponse<Void> reorder(@PathVariable UUID websiteId, @RequestBody List<UUID> serviceIds) {
        serviceItemService.reorder(websiteId, currentAccount.get(), serviceIds);
        return ApiResponse.ok(null, "Services reordered.");
    }
}
