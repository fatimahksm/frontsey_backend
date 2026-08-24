package com.dbwb.platform.website.entity;

/**
 * A structural page arrangement, orthogonal to both TemplateType (the
 * content model - menu vs. portfolio) and Theme (colors only). Each variant
 * renders the exact same underlying data with a genuinely different visual
 * shape. Owners can preview and switch anytime, same as Theme.
 */
public enum LayoutVariant {
    /**
     * Business-card header (logo/name/description), gallery strip, and a
     * categorized menu list with sub-categories. Deliberately display-only:
     * a plain price list customers read, with no cart and no ordering.
     */
    MENU_CLASSIC(TemplateType.MENU_ORDERING, true),
    /** Full-width cover hero, sticky category tabs, items as a card grid, cart as a slide-out drawer. */
    MENU_GRID(TemplateType.MENU_ORDERING, false),
    /**
     * Fine-dining style: display typography and a single-column list with
     * dotted price leaders. Deliberately display-only, and stripped back
     * further than Classic - it renders no gallery and no custom sections
     * either, only the masthead, the search field, and the dishes.
     */
    MENU_ELEGANT(TemplateType.MENU_ORDERING, true),
    /** Warm, photography-led cafe/bistro style: bold headline hero, real combo-box deals, card-grid menu with sticky category filters. */
    MENU_BISTRO(TemplateType.MENU_ORDERING, false),
    // The four portfolio names no longer describe their designs.
    //
    // They were rebuilt around four audiences rather than four looks, and the
    // decision at the time was to keep the stored names and change only the
    // labels - renaming them would have needed a migration mapping every
    // existing business_websites.layout_variant row, since an unmapped value
    // fails to deserialise on read. So the names below are historical, and the
    // comment on each says what it actually renders. The owner-facing labels
    // live in the frontend's lib/website/layout-options.ts, which is the only
    // place a person sees.

    /** Renders "Professional / CV" - experience, skills, projects and a downloadable CV, dense and dark. For developers, engineers, accountants, consultants. */
    PORTFOLIO_HERO(TemplateType.PORTFOLIO, true),
    /** Renders "Creative / Visual" - pictures first, large editorial compositions with a caption beside each, on warm paper. For designers, photographers, architects, artists. */
    PORTFOLIO_MINIMAL(TemplateType.PORTFOLIO, true),
    /** Renders "Brand / Product" - a loud front page for something you made: story, featured items, social links, heavy type. For small businesses, creators, studios, shops. */
    PORTFOLIO_BOLD(TemplateType.PORTFOLIO, true),
    /** Renders "Freelancer / Services" - built to get you booked: offers and prices, client proof, FAQ, a contact button never far away. For coaches, marketers, trainers, tutors. */
    PORTFOLIO_PROFILE(TemplateType.PORTFOLIO, true);

    private final TemplateType templateType;
    private final boolean displayOnly;

    LayoutVariant(TemplateType templateType, boolean displayOnly) {
        this.templateType = templateType;
        this.displayOnly = displayOnly;
    }

    public TemplateType templateType() {
        return templateType;
    }

    /**
     * True when the layout has no cart at all, so the website's OrderingMode
     * is forced to DISPLAY_ONLY while it is selected (BR-ORD-001). Selecting
     * such a layout is itself the "this is a read-only menu" decision - the
     * owner never has to also find and flip an ordering-mode switch.
     */
    public boolean isDisplayOnly() {
        return displayOnly;
    }

    public static LayoutVariant defaultFor(TemplateType templateType) {
        return templateType == TemplateType.PORTFOLIO ? PORTFOLIO_HERO : MENU_CLASSIC;
    }
}
