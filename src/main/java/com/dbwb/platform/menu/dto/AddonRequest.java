package com.dbwb.platform.menu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddonRequest(@NotBlank String name, @NotNull @DecimalMin("0.0") BigDecimal extraPrice) {
}
