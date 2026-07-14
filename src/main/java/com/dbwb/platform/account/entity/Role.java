package com.dbwb.platform.account.entity;

/**
 * BR-AUTH-005: the platform distinguishes Super Admin, Business Owner, Manager,
 * and Public Customer. Public Customer never has an account (BR-RULE-004),
 * so it is not represented here - only roles that authenticate.
 */
public enum Role {
    SUPER_ADMIN,
    BUSINESS_OWNER,
    MANAGER
}
