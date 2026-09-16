package com.dbwb.platform.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The part of an account its owner may change about themselves.
 *
 * The email is not here on purpose: it is the login and the address every
 * verification and reset is sent to, so changing it is a flow of its own -
 * confirm at the new address before it takes effect - rather than a field on a
 * settings form. Offering it here would let someone lock themselves out with a
 * typo.
 */
public record UpdateAccountProfileRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 255, message = "is too long")
        String fullName
) {
}
