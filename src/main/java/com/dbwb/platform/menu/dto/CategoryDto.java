package com.dbwb.platform.menu.dto;

import com.dbwb.platform.menu.entity.Category;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CategoryDto(UUID id, @NotBlank String name) {
    public static CategoryDto from(Category c) {
        return new CategoryDto(c.getId(), c.getName());
    }
}
