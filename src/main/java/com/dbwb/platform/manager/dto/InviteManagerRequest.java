package com.dbwb.platform.manager.dto;

import com.dbwb.platform.manager.entity.Permission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

/** BR-MGR-001/003: invite by email with a set of granular permissions chosen by the Owner. */
public record InviteManagerRequest(
        @NotBlank @Email String email,
        @NotEmpty Set<Permission> permissions
) {
}
