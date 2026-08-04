package com.dbwb.platform.menu.dto;

import java.util.List;

/** BR-IMP-004: if false and any INVALID rows are present, the whole import is rejected instead of partially applied. */
public record ConfirmImportRequest(boolean importValidRowsOnly, List<ImportRowDecision> duplicateDecisions) {
}
