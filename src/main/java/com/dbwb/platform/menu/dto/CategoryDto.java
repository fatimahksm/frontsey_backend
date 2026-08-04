package com.dbwb.platform.menu.dto;

import com.dbwb.platform.menu.entity.Category;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/** {@code parentId} is null for a top-level category, set for a sub-category. */
public record CategoryDto(UUID id, @NotBlank String name, UUID parentId) {
    public static CategoryDto from(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getParent() == null ? null : c.getParent().getId());
    }
}
