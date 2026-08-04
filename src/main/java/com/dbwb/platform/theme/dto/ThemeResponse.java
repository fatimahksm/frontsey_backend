package com.dbwb.platform.theme.dto;

import com.dbwb.platform.theme.entity.Theme;

import java.util.UUID;

public record ThemeResponse(
        UUID id,
        String name,
        String description,
        String themeConfig
) {
    public static ThemeResponse from(Theme theme) {
        return new ThemeResponse(theme.getId(), theme.getName(), theme.getDescription(), theme.getThemeConfig());
    }
}
