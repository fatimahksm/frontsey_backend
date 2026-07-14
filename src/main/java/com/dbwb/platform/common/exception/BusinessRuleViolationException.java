package com.dbwb.platform.common.exception;

/**
 * Thrown when a request is well-formed but violates an explicit BRD business rule
 * (e.g. BR-RULE-001: cannot publish without an active subscription).
 */
public class BusinessRuleViolationException extends RuntimeException {
    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
