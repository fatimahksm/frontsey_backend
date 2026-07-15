package com.dbwb.platform.menu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BoxVariantRequest(
        @NotBlank String label,
        @Min(1) int unitCount,
        @NotNull @DecimalMin("0.0") BigDecimal price
) {
}
