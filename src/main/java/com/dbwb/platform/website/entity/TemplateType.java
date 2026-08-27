package com.dbwb.platform.website.entity;

/**
 * The structural shape of a website, chosen at creation - distinct from
 * Theme, which only governs visual styling. Each type has its own content
 * model and its own mandatory-publication rules (see
 * WebsiteService.validateMandatoryPublicationFields).
 */
public enum TemplateType {
    /** Categories/items with sizes, add-ons, box variants, and optional WhatsApp cart ordering. */
    MENU_ORDERING,
    /** A simple services showcase for businesses that don't sell a menu (salons, studios, agencies...). */
    PORTFOLIO,
    /**
     * A single occasion and the pictures from it - a wedding, an engagement, a
     * graduation, a party.
     *
     * Its own type rather than a portfolio layout because the questions are
     * different: an event has one date, one venue and a running order, none of
     * which a services showcase has anywhere to put.
     */
    EVENTS
}
