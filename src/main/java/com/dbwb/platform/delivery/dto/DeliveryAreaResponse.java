package com.dbwb.platform.delivery.dto;

import com.dbwb.platform.delivery.entity.DeliveryArea;

import java.math.BigDecimal;
import java.util.UUID;

public record DeliveryAreaResponse(
        UUID id,
        String name,
        BigDecimal deliveryFee,
        BigDecimal minimumOrderAmount,
        BigDecimal freeDeliveryThreshold
) {
    public static DeliveryAreaResponse from(DeliveryArea area) {
        return new DeliveryAreaResponse(
                area.getId(), area.getName(), area.getDeliveryFee(),
                area.getMinimumOrderAmount(), area.getFreeDeliveryThreshold());
    }
}
