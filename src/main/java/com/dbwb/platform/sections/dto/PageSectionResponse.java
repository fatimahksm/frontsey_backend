package com.dbwb.platform.sections.dto;

import com.dbwb.platform.sections.entity.PageSection;
import com.dbwb.platform.sections.entity.PageSectionType;

import java.util.UUID;

public record PageSectionResponse(
        UUID id,
        PageSectionType type,
        String data,
        int sortOrder
) {
    public static PageSectionResponse from(PageSection section) {
        return new PageSectionResponse(section.getId(), section.getType(), section.getData(), section.getSortOrder());
    }
}
