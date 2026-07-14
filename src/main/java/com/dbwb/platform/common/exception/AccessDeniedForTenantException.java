package com.dbwb.platform.common.exception;

/**
 * Thrown whenever an Account attempts to act on a Business Website it does not
 * own and does not have Manager permission for (BR-RULE-003, tenant isolation).
 */
public class AccessDeniedForTenantException extends RuntimeException {
    public AccessDeniedForTenantException(String message) {
        super(message);
    }
}
