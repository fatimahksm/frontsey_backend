package com.dbwb.platform.manager.dto;

import com.dbwb.platform.manager.entity.InvitationStatus;
import com.dbwb.platform.manager.entity.ManagerAccess;
import com.dbwb.platform.manager.entity.Permission;

import java.util.Set;
import java.util.UUID;

/** Phase 4: an invitation from the invited person's point of view - which website, not just the raw access-record fields. */
public record ManagerInvitationResponse(
        UUID id,
        UUID websiteId,
        String businessName,
        InvitationStatus status,
        Set<Permission> permissions
) {
    public static ManagerInvitationResponse from(ManagerAccess a) {
        return new ManagerInvitationResponse(a.getId(), a.getWebsite().getId(), a.getWebsite().getBusinessName(), a.getStatus(), a.getPermissions());
    }
}
