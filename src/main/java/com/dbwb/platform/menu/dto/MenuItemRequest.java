package com.dbwb.platform.menu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record MenuItemRequest(
        @NotNull UUID categoryId,
        @NotBlank String name,
        String description,
        String ingredients,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal price,
        BigDecimal discountPrice,
        String imageUrl,
        Integer maxOrderQuantity,
        /** BR-OPT-004: true if this item is a fixed box (uses BoxVariants, not SizeVariants). */
        boolean fixedBoxItem
) {
}
