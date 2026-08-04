package com.dbwb.platform.support.entity;

import com.dbwb.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

/** BRD 9.15: contact-form support ticket submitted by an authenticated account (TBD-012 for category list / attachment limits). */
@Entity
@Table(name = "support_tickets")
public class SupportTicket extends BaseEntity {

    @Column(nullable = false)
    private UUID submitterAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupportCategory category;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    private String attachmentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupportTicketStatus status = SupportTicketStatus.OPEN;

    public UUID getSubmitterAccountId() {
        return submitterAccountId;
    }

    public void setSubmitterAccountId(UUID submitterAccountId) {
        this.submitterAccountId = submitterAccountId;
    }

    public SupportCategory getCategory() {
        return category;
    }

    public void setCategory(SupportCategory category) {
        this.category = category;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public SupportTicketStatus getStatus() {
        return status;
    }

    public void setStatus(SupportTicketStatus status) {
        this.status = status;
    }
}
