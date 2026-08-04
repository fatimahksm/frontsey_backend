package com.dbwb.platform.account.entity;

public enum TokenType {
    EMAIL_VERIFICATION,
    PASSWORD_RESET,
    /** BR-AUTH-007: long-lived, rotated-on-use token that lets the frontend silently obtain a new access token without forcing re-login. */
    REFRESH
}
