package com.dbwb.platform.menu.dto;

import com.dbwb.platform.menu.entity.SizeVariant;

import java.math.BigDecimal;
import java.util.UUID;

public record SizeVariantResponse(UUID id, String label, BigDecimal price) {
    public static SizeVariantResponse from(SizeVariant v) {
        return new SizeVariantResponse(v.getId(), v.getLabel(), v.getPrice());
    }
}
