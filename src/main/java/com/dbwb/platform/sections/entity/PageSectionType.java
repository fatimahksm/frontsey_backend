package com.dbwb.platform.sections.entity;

/**
 * The closed set of extra page sections an owner can add on top of the
 * fixed, template-driven content (menu/services, gallery, contact). Each
 * type has its own JSON data shape (see PageSectionRequest/Response) that
 * every layout design renders in its own visual style.
 */
public enum PageSectionType {
    ABOUT,
    TESTIMONIALS,
    FAQ,
    TEAM
}
