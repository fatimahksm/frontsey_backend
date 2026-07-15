package com.dbwb.platform.admin.dto;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.AccountStatus;
import com.dbwb.platform.account.entity.Role;

import java.time.Instant;
import java.util.UUID;

public record AccountSummaryResponse(
        UUID id,
        String email,
        String fullName,
        Role role,
        AccountStatus status,
        boolean emailVerified,
        Instant createdAt
) {
    public static AccountSummaryResponse from(Account account) {
        return new AccountSummaryResponse(
                account.getId(), account.getEmail(), account.getFullName(), account.getRole(),
                account.getStatus(), account.isEmailVerified(), account.getCreatedAt());
    }
}
