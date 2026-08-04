package com.dbwb.platform.menu.dto;

import jakarta.validation.constraints.NotNull;

/** BR-IMP-003: the Owner's explicit choice for one DUPLICATE row from the preview. Ignored for VALID/INVALID rows. */
public record ImportRowDecision(int rowNumber, @NotNull DuplicateAction action) {
}
