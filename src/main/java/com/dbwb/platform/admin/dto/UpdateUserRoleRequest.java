package com.dbwb.platform.admin.dto;

import com.dbwb.platform.account.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(@NotNull Role role) {
}
