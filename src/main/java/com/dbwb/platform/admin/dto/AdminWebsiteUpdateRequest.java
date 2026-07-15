package com.dbwb.platform.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminWebsiteUpdateRequest(@NotBlank String businessName) {
}
