package com.dbwb.platform.menu.dto;

/** BR-IMP-003: no silent overwrite - the Owner explicitly picks one, per duplicate row. */
public enum DuplicateAction {
    UPDATE,
    SKIP
}
