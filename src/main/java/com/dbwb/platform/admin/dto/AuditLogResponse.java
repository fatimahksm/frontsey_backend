package com.dbwb.platform.admin.dto;

import com.dbwb.platform.audit.entity.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorAccountId,
        String actorEmail,
        String action,
        String targetId,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog log, String actorEmail) {
        return new AuditLogResponse(log.getId(), log.getActorAccountId(), actorEmail, log.getAction(), log.getTargetId(), log.getCreatedAt());
    }
}
