package com.dbwb.platform.portfolio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record ServiceItemRequest(
        @NotBlank String name,
        String description,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal price, // null => priced on request
        String imageUrl
) {
}
