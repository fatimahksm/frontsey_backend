package com.dbwb.platform.menu.dto;

import java.util.List;

/** BR-IMP-002: the full data preview + validation summary shown before the Owner confirms. */
public record ImportPreviewResponse(
        int totalRows,
        int validCount,
        int invalidCount,
        int duplicateCount,
        List<ImportRowResult> rows
) {
    public static ImportPreviewResponse of(List<ImportRowResult> rows) {
        int valid = (int) rows.stream().filter(r -> r.status() == ImportRowStatus.VALID).count();
        int invalid = (int) rows.stream().filter(r -> r.status() == ImportRowStatus.INVALID).count();
        int duplicate = (int) rows.stream().filter(r -> r.status() == ImportRowStatus.DUPLICATE).count();
        return new ImportPreviewResponse(rows.size(), valid, invalid, duplicate, rows);
    }
}
