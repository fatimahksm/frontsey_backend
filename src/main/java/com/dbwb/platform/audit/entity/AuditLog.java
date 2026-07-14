package com.dbwb.platform.audit.entity;

import com.dbwb.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * BR-AUD-001 / Section 14: records actor, action, target, and timestamp
 * (timestamp comes from BaseEntity.createdAt) for significant operations -
 * price changes, publication, access changes, plan changes, suspension, etc.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @Column(nullable = false)
    private UUID actorAccountId;

    @Column(nullable = false)
    private String action;

    private String targetId;

    public UUID getActorAccountId() {
        return actorAccountId;
    }

    public void setActorAccountId(UUID actorAccountId) {
        this.actorAccountId = actorAccountId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }
}
