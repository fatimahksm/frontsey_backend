package com.dbwb.platform.manager.dto;

import com.dbwb.platform.manager.entity.InvitationStatus;
import com.dbwb.platform.manager.entity.ManagerAccess;
import com.dbwb.platform.manager.entity.Permission;

import java.util.Set;
import java.util.UUID;

public record ManagerAccessResponse(UUID id, String invitedEmail, InvitationStatus status, Set<Permission> permissions) {
    public static ManagerAccessResponse from(ManagerAccess a) {
        return new ManagerAccessResponse(a.getId(), a.getInvitedEmail(), a.getStatus(), a.getPermissions());
    }
}
