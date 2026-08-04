package com.dbwb.platform.portfolio.dto;

import com.dbwb.platform.portfolio.entity.ServiceItem;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceItemResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        int sortOrder
) {
    public static ServiceItemResponse from(ServiceItem service) {
        return new ServiceItemResponse(
                service.getId(), service.getName(), service.getDescription(),
                service.getPrice(), service.getImageUrl(), service.getSortOrder());
    }
}
