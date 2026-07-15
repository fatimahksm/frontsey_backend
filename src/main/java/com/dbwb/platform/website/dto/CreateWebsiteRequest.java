package com.dbwb.platform.website.dto;

import com.dbwb.platform.website.entity.PageMode;
import com.dbwb.platform.website.entity.TemplateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** BR-SITE-001..004: first wizard step - name, page mode, template type, and theme choice. */
public record CreateWebsiteRequest(
        @NotBlank String businessName,
        @NotNull PageMode pageMode,
        @NotNull TemplateType templateType,
        UUID themeId // null => build-from-scratch (BR-SITE-002)
) {
}
