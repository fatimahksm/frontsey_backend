package com.dbwb.platform.website.dto;

/**
 * The website's own ThemeConfig JSON, or null to clear the override and go
 * back to inheriting the selected preset. Deliberately a raw string rather
 * than a typed ThemeConfig: the column is TEXT and ThemeConfigValidator is
 * the single place that decides what counts as valid theme JSON, for the
 * admin preset path and this one alike.
 */
public record UpdateThemeConfigRequest(String themeConfig) {
}
