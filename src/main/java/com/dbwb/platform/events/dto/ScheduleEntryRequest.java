package com.dbwb.platform.events.dto;

import jakarta.validation.constraints.NotBlank;

/** The title is the one thing a line in the running order cannot do without. */
public record ScheduleEntryRequest(
        String time,
        @NotBlank String title,
        String detail
) {
}
