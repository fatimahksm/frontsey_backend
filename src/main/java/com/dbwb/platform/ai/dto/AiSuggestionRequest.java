package com.dbwb.platform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiSuggestionRequest(
        @NotBlank String businessName,
        /** "MENU_ORDERING" or "PORTFOLIO" - optional context, not validated against the enum since this is advisory only. */
        String templateType,
        @NotNull AiSuggestionFieldType fieldType,
        /** Optional existing draft text to refine rather than write from scratch. */
        String existingText
) {
}
