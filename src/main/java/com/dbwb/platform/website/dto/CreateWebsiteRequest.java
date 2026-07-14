package com.dbwb.platform.website.dto;

import com.dbwb.platform.website.entity.PageMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** BR-SITE-001..004: first wizard step - name, page mode, and theme choice. */
public record CreateWebsiteRequest(
        @NotBlank String businessName,
        @NotNull PageMode pageMode,
        UUID themeId // null => build-from-scratch (BR-SITE-002)
) {
}
