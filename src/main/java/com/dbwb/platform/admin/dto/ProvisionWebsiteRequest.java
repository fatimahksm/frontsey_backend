package com.dbwb.platform.admin.dto;

import com.dbwb.platform.website.entity.LayoutVariant;
import com.dbwb.platform.website.entity.PageMode;
import com.dbwb.platform.website.entity.TemplateType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A Super Admin standing a website up on someone else's behalf.
 *
 * The owner is named by email rather than by id on purpose: the whole point is
 * that the admin is setting this up for a person who may not have an account
 * yet. One is created for them if it does not exist, and they are invited to
 * set their own password - the admin never chooses it and never sees it.
 */
public record ProvisionWebsiteRequest(
        @NotBlank @Email @Size(max = 255) String ownerEmail,
        /** Used only when the account has to be created; ignored for an existing owner. */
        @Size(max = 255) String ownerFullName,
        @NotBlank @Size(max = 120) String businessName,
        @NotNull TemplateType templateType,
        /** Optional - defaults to the template type's own default layout. */
        LayoutVariant layoutVariant,
        /** Optional - defaults to ONE_PAGE. */
        PageMode pageMode,
        /** True to grant this website free access: never billed, never expires. */
        boolean complimentary
) {
}
