package com.dbwb.platform.account.dto;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.AccountStatus;
import com.dbwb.platform.account.entity.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * Who you are signed in as.
 *
 * There was nowhere to ask. The console could tell you which website you were
 * editing but never your own name, your email, or which role you were using -
 * so the Account screen offered to export and delete an account it never
 * identified, and a Super Admin had no route to it at all.
 *
 * No password hash and no token, obviously; this is what a person may read
 * about themselves.
 */
public record AccountProfileResponse(
        UUID id,
        String email,
        String fullName,
        Role role,
        AccountStatus status,
        boolean emailVerified,
        /** Null until a deletion is requested; a date here means the account is scheduled to go. */
        Instant disabledAt,
        Instant createdAt
) {
    public static AccountProfileResponse from(Account account) {
        return new AccountProfileResponse(
                account.getId(), account.getEmail(), account.getFullName(), account.getRole(),
                account.getStatus(), account.isEmailVerified(), account.getDisabledAt(), account.getCreatedAt());
    }
}
