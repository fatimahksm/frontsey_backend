package com.dbwb.platform.menu.dto;

import com.dbwb.platform.menu.entity.ItemAvailability;
import com.dbwb.platform.menu.entity.MenuItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MenuItemResponse(
        UUID id,
        UUID categoryId,
        String name,
        String description,
        String ingredients,
        BigDecimal price,
        BigDecimal discountPrice,
        String imageUrl,
        ItemAvailability availability,
        Instant unavailableUntil,
        Integer maxOrderQuantity,
        boolean fixedBoxItem
) {
    public static MenuItemResponse from(MenuItem item) {
        return new MenuItemResponse(
                item.getId(), item.getCategory().getId(), item.getName(), item.getDescription(),
                item.getIngredients(), item.getPrice(), item.getDiscountPrice(), item.getImageUrl(),
                item.getAvailability(), item.getUnavailableUntil(), item.getMaxOrderQuantity(),
                item.isFixedBoxItem());
    }
}
