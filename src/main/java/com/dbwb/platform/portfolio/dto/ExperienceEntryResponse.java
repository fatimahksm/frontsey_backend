package com.dbwb.platform.portfolio.dto;

import com.dbwb.platform.portfolio.entity.ExperienceEntry;

import java.util.UUID;

public record ExperienceEntryResponse(
        UUID id,
        String role,
        String company,
        String year,
        String detail,
        int sortOrder
) {
    public static ExperienceEntryResponse from(ExperienceEntry entry) {
        return new ExperienceEntryResponse(
                entry.getId(), entry.getRole(), entry.getCompany(), entry.getYear(),
                entry.getDetail(), entry.getSortOrder());
    }
}
