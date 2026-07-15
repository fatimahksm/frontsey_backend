package com.dbwb.platform.support.dto;

import com.dbwb.platform.support.entity.SupportCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitSupportTicketRequest(
        @NotNull SupportCategory category,
        @NotBlank String subject,
        @NotBlank String message,
        String attachmentUrl
) {
}
