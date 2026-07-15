package com.dbwb.platform.menu.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ImportRowResult(
        int rowNumber,
        String categoryName,
        String name,
        String description,
        String ingredients,
        BigDecimal price,
        BigDecimal discountPrice,
        String imageUrl,
        Integer maxOrderQuantity,
        ImportRowStatus status,
        List<String> errors,
        /** Set only when status = DUPLICATE - the existing item this row's name collides with. */
        UUID existingItemId
) {
}
