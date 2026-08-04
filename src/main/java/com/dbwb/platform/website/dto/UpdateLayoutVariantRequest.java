package com.dbwb.platform.website.dto;

import com.dbwb.platform.website.entity.LayoutVariant;
import jakarta.validation.constraints.NotNull;

public record UpdateLayoutVariantRequest(@NotNull LayoutVariant layoutVariant) {
}
