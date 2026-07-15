package com.dbwb.platform.menu.dto;

import com.dbwb.platform.menu.entity.BoxVariant;

import java.math.BigDecimal;
import java.util.UUID;

public record BoxVariantResponse(UUID id, String label, int unitCount, BigDecimal price) {
    public static BoxVariantResponse from(BoxVariant v) {
        return new BoxVariantResponse(v.getId(), v.getLabel(), v.getUnitCount(), v.getPrice());
    }
}
