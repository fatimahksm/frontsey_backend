package com.dbwb.platform.sections.dto;

import com.dbwb.platform.sections.entity.PageSectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** `data` is opaque JSON whose shape depends on `type` - structured/validated on the frontend. */
public record PageSectionRequest(
        @NotNull PageSectionType type,
        @NotBlank String data
) {
}
