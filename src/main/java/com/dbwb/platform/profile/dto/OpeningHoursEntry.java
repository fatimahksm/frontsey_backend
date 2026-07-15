package com.dbwb.platform.profile.dto;

import com.dbwb.platform.profile.entity.OpeningHours;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** BR-HRS-001/002: one entry per day of the week, used for both requests and responses. */
public record OpeningHoursEntry(
        @NotNull DayOfWeek dayOfWeek,
        boolean open,
        LocalTime opensAt,
        LocalTime closesAt
) {
    public static OpeningHoursEntry from(OpeningHours h) {
        return new OpeningHoursEntry(h.getDayOfWeek(), h.isOpen(), h.getOpensAt(), h.getClosesAt());
    }
}
