package com.dbwb.platform.admin.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/** BR-ADM-003/004: a suspension always needs a reason and a temporary/permanent classification. */
public record SuspendWebsiteRequest(
        @NotBlank String reason,
        boolean permanent,
        /** Required (and only meaningful) when permanent = false; null means "manual reactivation only". */
        Instant reactivateAt
) {
}
