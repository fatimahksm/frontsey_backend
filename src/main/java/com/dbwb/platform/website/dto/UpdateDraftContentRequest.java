package com.dbwb.platform.website.dto;

import com.dbwb.platform.website.entity.OrderingMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * BR-THEME-004/005: editor content is always saved against the draft first
 * (auto-save or explicit Save) and never affects the public site until Publish.
 * "content" is opaque structured JSON produced by the frontend builder
 * (sections, branding, language/currency choices, etc.). orderingMode is a
 * first-class field rather than buried in that opaque blob because
 * WebsiteService.validateMandatoryPublicationFields (BR-RULE-013) and the
 * public storefront both need to branch on it directly.
 */
public record UpdateDraftContentRequest(@NotBlank String content, @NotNull OrderingMode orderingMode) {
}
