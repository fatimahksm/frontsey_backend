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
     * A shop and the things it sells: collections, products, prices, and
     * optional WhatsApp ordering.
     *
     * It shares MENU_ORDERING's tables - a collection is a category and a
     * product is an item - but it is its own type because the two are not the
     * same business. A menu is read at a table by someone who already chose
     * the place; a shop is browsed by someone deciding whether to buy at all,
     * so the photograph, the price and whether a thing is in stock carry the
     * page, while sizes, add-ons and box deals mostly do not apply.
     *
     * Before this existed a shop was a MENU_ORDERING website with a
     * "menuBusinessKind: SHOP" flag in its draft content, which renamed labels
     * and swapped the sample photos but still gave a shop a restaurant's
     * layout. Those websites keep working exactly as they did; the flag is no
     * longer offered to anyone creating a website now.
     */
    STORE,
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
