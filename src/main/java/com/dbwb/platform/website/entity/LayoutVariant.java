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
    // These four were HERO / MINIMAL / BOLD / PROFILE until V19 renamed them.
    // The templates had been rebuilt around four audiences and the old names
    // described the designs they replaced, so each name said the opposite of
    // what it selected. The owner-facing labels live in the frontend's
    // lib/website/layout-options.ts; these are the stored values.

    /** Renders "Professional / CV" - experience, skills, projects and a downloadable CV, dense and dark. For developers, engineers, accountants, consultants. */
    PORTFOLIO_PROFESSIONAL(TemplateType.PORTFOLIO, true),
    /** Renders "Creative / Visual" - pictures first, large editorial compositions with a caption beside each, on warm paper. For designers, photographers, architects, artists. */
    PORTFOLIO_VISUAL(TemplateType.PORTFOLIO, true),
    /** Renders "Brand / Product" - a loud front page for something you made: story, featured items, social links, heavy type. For small businesses, creators, studios, shops. */
    PORTFOLIO_BRAND(TemplateType.PORTFOLIO, true),
    /** Renders "Freelancer / Services" - built to get you booked: offers and prices, client proof, FAQ, a contact button never far away. For coaches, marketers, trainers, tutors. */
    PORTFOLIO_SERVICES(TemplateType.PORTFOLIO, true),

    /**
     * One occasion, told in order: who and what, when and where, the running
     * order of the day, and the photographs afterwards. Display-only - an
     * invitation has nothing to sell.
     */
    EVENTS_CELEBRATION(TemplateType.EVENTS, true);

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
        return switch (templateType) {
            case PORTFOLIO -> PORTFOLIO_PROFESSIONAL;
            case EVENTS -> EVENTS_CELEBRATION;
            case MENU_ORDERING -> MENU_CLASSIC;
        };
    }
}
