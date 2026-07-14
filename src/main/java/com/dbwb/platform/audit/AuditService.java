package com.dbwb.platform.audit;

import com.dbwb.platform.audit.entity.AuditLog;
import com.dbwb.platform.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** BR-AUD-001: single, consistent entry point for writing audit records. */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(UUID actorAccountId, String action, String targetId) {
        AuditLog log = new AuditLog();
        log.setActorAccountId(actorAccountId);
        log.setAction(action);
        log.setTargetId(targetId);
        auditLogRepository.save(log);
    }
}
