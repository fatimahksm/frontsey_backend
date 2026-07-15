package com.dbwb.platform.admin.dto;

import jakarta.validation.constraints.NotBlank;

/** BR-ADM-006: Super Admin add/edit of a theme. */
public record ThemeRequest(
        @NotBlank String name,
        String description,
        String themeConfig,
        boolean active
) {
}
