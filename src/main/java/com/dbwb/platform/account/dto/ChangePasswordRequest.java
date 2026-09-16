package com.dbwb.platform.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Changing your password while signed in.
 *
 * The current password is required even though the caller is already
 * authenticated: a live session on an unattended machine is exactly the case
 * this stops, and without it anyone who found a signed-in browser could lock
 * the real owner out of their own business.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "is required")
        String currentPassword,

        @NotBlank(message = "is required")
        @Size(min = 8, message = "must be at least 8 characters")
        String newPassword
) {
}
