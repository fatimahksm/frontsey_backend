package com.dbwb.platform.account.entity;

/**
 * BR-AUTH-006: deletion is a two-step process - the account is disabled first,
 * then permanently deleted after the configured retention window.
 */
public enum AccountStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    DISABLED_PENDING_DELETION,
    DELETED
}
