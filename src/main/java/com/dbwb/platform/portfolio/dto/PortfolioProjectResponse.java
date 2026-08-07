package com.dbwb.platform.portfolio.dto;

import com.dbwb.platform.portfolio.entity.PortfolioProject;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record PortfolioProjectResponse(
        UUID id,
        String name,
        String discipline,
        String year,
        String summary,
        /** Split from the stored comma-separated column, so clients never parse it themselves. */
        List<String> tags,
        String imageUrl,
        String liveUrl,
        String repoUrl,
        int sortOrder
) {
    public static PortfolioProjectResponse from(PortfolioProject p) {
        return new PortfolioProjectResponse(
                p.getId(), p.getName(), p.getDiscipline(), p.getYear(), p.getSummary(),
                splitTags(p.getTags()), p.getImageUrl(), p.getLiveUrl(), p.getRepoUrl(), p.getSortOrder());
    }

    /** Empty rather than a list containing "" - templates check length, not content. */
    public static List<String> splitTags(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
