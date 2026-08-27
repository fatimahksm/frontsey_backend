package com.dbwb.platform.events.dto;

import com.dbwb.platform.events.entity.EventScheduleEntry;

import java.util.UUID;

public record ScheduleEntryResponse(UUID id, String time, String title, String detail, int sortOrder) {
    public static ScheduleEntryResponse from(EventScheduleEntry entry) {
        return new ScheduleEntryResponse(
                entry.getId(), entry.getTime(), entry.getTitle(), entry.getDetail(), entry.getSortOrder());
    }
}
