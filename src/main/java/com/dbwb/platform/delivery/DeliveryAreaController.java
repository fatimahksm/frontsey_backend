package com.dbwb.platform.delivery;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.delivery.dto.DeliveryAreaResponse;
import com.dbwb.platform.security.CurrentAccount;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/websites/{websiteId}/delivery-areas")
public class DeliveryAreaController {

    private final DeliveryAreaService service;
    private final CurrentAccount currentAccount;

    public DeliveryAreaController(DeliveryAreaService service, CurrentAccount currentAccount) {
        this.service = service;
        this.currentAccount = currentAccount;
    }

    @PostMapping
    public ApiResponse<DeliveryAreaResponse> create(@PathVariable UUID websiteId,
                                     @RequestParam @NotBlank String name,
                                     @RequestParam BigDecimal fee,
                                     @RequestParam BigDecimal minimumOrder,
                                     @RequestParam(required = false) BigDecimal freeThreshold) {
        var area = service.create(websiteId, currentAccount.get(), name, fee, minimumOrder, freeThreshold);
        return ApiResponse.ok(DeliveryAreaResponse.from(area));
    }

    @GetMapping
    public ApiResponse<List<DeliveryAreaResponse>> list(@PathVariable UUID websiteId) {
        return ApiResponse.ok(service.list(websiteId, currentAccount.get())
                .stream().map(DeliveryAreaResponse::from).toList());
    }

    @DeleteMapping("/{areaId}")
    public ApiResponse<Void> delete(@PathVariable UUID websiteId, @PathVariable UUID areaId) {
        service.delete(websiteId, areaId, currentAccount.get());
        return ApiResponse.ok(null, "Delivery area deleted.");
    }
}
