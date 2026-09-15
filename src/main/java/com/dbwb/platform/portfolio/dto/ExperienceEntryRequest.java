package com.dbwb.platform.portfolio.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Create/update payload for one line of work history. Only the role is
 * required: the templates render whatever else is present and hide the rest,
 * so an owner can put the job title down now and the dates in later.
 */
public record ExperienceEntryRequest(
        @NotBlank String role,
        String company,
        String year,
        String detail
) {
}
