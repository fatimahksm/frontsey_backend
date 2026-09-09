package com.dbwb.platform.events.dto;

/** Every field optional: an invitation that says only "Saturday, at ours" is a real invitation. */
public record EventDetailsRequest(
        String eventDate,
        String startTime,
        String endTime,
        String venueName,
        String dressCode,
        String rsvpBy,
        String note
) {
}
