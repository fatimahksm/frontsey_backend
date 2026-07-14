package com.dbwb.platform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** BR-AUTH-001: registration requires a unique, valid email and a password. */
public record RegisterRequest(

        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters long")
        String password,

        @NotBlank
        String fullName
) {
}
