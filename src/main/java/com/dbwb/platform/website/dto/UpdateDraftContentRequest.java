package com.dbwb.platform.website.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * BR-THEME-004/005: editor content is always saved against the draft first
 * (auto-save or explicit Save) and never affects the public site until Publish.
 * "content" is opaque structured JSON produced by the frontend builder
 * (sections, branding, ordering mode, language/currency choices, etc.).
 */
public record UpdateDraftContentRequest(@NotBlank String content) {
}
