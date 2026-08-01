package com.dbwb.platform.theme.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Phase 3: the single strongly-typed schema for a template's design system.
 * Replaces the previously-arbitrary {@code Theme.themeConfig} free-text JSON
 * blob - every value stored in that column must parse into exactly this
 * shape and pass these constraints (see {@code ThemeConfigValidator}), so
 * there is no longer any theme JSON the public renderer silently ignores.
 */
public record ThemeConfig(
        @NotNull FontChoice fontFamily,
        @NotNull FontChoice headingFontFamily,
        @NotBlank @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "must be a 6-digit hex color") String primaryColor,
        @NotBlank @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "must be a 6-digit hex color") String secondaryColor,
        @NotBlank @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "must be a 6-digit hex color") String backgroundColor,
        @NotBlank @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "must be a 6-digit hex color") String surfaceColor,
        @NotBlank @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "must be a 6-digit hex color") String textColor,
        @NotNull ButtonStyle buttonStyle,
        @NotNull CardStyle cardStyle,
        @Min(0) @Max(32) int borderRadius,
        @NotNull SectionSpacing sectionSpacing
) {
    public enum FontChoice { SYSTEM_SANS, MODERN_SANS, ELEGANT_SERIF, CLASSIC_SERIF, MONOSPACE }

    public enum ButtonStyle { ROUNDED, PILL, SQUARE }

    public enum CardStyle { FLAT, SOFT_SHADOW, BORDERED }

    public enum SectionSpacing { COMPACT, COMFORTABLE, SPACIOUS }

    /** Used for "build from scratch" websites (themeId = null) so there's never a null-theme case to special-case. */
    public static ThemeConfig defaults() {
        return new ThemeConfig(
                FontChoice.SYSTEM_SANS, FontChoice.SYSTEM_SANS,
                "#7c3aed", "#f4f0fb", "#ffffff", "#ffffff", "#0a0a0f",
                ButtonStyle.PILL, CardStyle.SOFT_SHADOW, 16, SectionSpacing.COMFORTABLE);
    }
}
