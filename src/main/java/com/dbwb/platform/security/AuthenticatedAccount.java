package com.dbwb.platform.security;

import com.dbwb.platform.account.entity.Role;

import java.util.UUID;

/**
 * Lightweight principal held in the SecurityContext for the duration of a request.
 * Kept minimal (no lazy JPA associations) since it is built directly from JWT claims.
 */
public record AuthenticatedAccount(UUID accountId, String email, Role role) {
}
