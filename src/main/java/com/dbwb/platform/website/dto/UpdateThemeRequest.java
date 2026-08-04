package com.dbwb.platform.website.dto;

import java.util.UUID;

/** BR-SITE-002: an Owner may switch themes (or go back to build-from-scratch with themeId = null) at any time. */
public record UpdateThemeRequest(UUID themeId) {
}
