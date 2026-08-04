package com.dbwb.platform.menu.dto;

import java.util.List;

/** BR-IMP-004: skipped/invalid rows stay visible here rather than disappearing silently. */
public record ImportOutcomeResponse(
        int createdCount,
        int updatedCount,
        int skippedCount,
        List<ImportRowResult> skippedRows
) {
}
