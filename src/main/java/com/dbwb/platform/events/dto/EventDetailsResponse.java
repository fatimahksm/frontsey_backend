package com.dbwb.platform.events.dto;

import com.dbwb.platform.events.entity.EventDetails;

public record EventDetailsResponse(
        String eventDate,
        String startTime,
        String endTime,
        String venueName,
        String dressCode,
        String rsvpBy,
        String note
) {
    public static EventDetailsResponse from(EventDetails details) {
        return new EventDetailsResponse(
                details.getEventDate(), details.getStartTime(), details.getEndTime(),
                details.getVenueName(), details.getDressCode(), details.getRsvpBy(), details.getNote());
    }

    /** What a website with nothing filled in yet returns - the editor then has a shape to bind to. */
    public static EventDetailsResponse empty() {
        return new EventDetailsResponse(null, null, null, null, null, null, null);
    }
}
