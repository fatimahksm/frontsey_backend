package com.dbwb.platform.notification.dto;

import com.dbwb.platform.notification.entity.Notification;
import com.dbwb.platform.notification.entity.NotificationEvent;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationEvent event,
        String message,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getEvent(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }
}
