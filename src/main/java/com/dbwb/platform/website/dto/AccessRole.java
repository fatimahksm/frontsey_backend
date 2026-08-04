package com.dbwb.platform.website.dto;

/** Phase 4: the caller's relationship to a website, exposed on WebsiteResponse so the frontend can gate UI without re-deriving it. */
public enum AccessRole {
    OWNER,
    MANAGER
}
