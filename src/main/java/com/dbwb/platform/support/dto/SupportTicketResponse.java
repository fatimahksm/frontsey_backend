package com.dbwb.platform.support.dto;

import com.dbwb.platform.support.entity.SupportCategory;
import com.dbwb.platform.support.entity.SupportTicket;
import com.dbwb.platform.support.entity.SupportTicketStatus;

import java.time.Instant;
import java.util.UUID;

public record SupportTicketResponse(
        UUID id,
        SupportCategory category,
        String subject,
        String message,
        String attachmentUrl,
        SupportTicketStatus status,
        Instant createdAt
) {
    public static SupportTicketResponse from(SupportTicket ticket) {
        return new SupportTicketResponse(
                ticket.getId(), ticket.getCategory(), ticket.getSubject(), ticket.getMessage(),
                ticket.getAttachmentUrl(), ticket.getStatus(), ticket.getCreatedAt());
    }
}
