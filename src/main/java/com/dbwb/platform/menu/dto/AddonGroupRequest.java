package com.dbwb.platform.menu.dto;

import jakarta.validation.constraints.NotBlank;

public record AddonGroupRequest(@NotBlank String name, Integer maxSelections) {
}
