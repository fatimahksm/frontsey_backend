package com.dbwb.platform.portfolio.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Create/update payload for a project. Only the name is required - the
 * templates render whatever else is present and hide the rest, so an owner can
 * add a project with a picture and fill the detail in later.
 */
public record PortfolioProjectRequest(
        @NotBlank String name,
        String discipline,
        String year,
        String summary,
        String tags,
        String imageUrl,
        String liveUrl,
        String repoUrl
) {
}
