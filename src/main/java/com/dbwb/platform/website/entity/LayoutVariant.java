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
    /** Fine-dining style: display typography, single-column list with dotted price leaders, fixed bottom cart bar. */
    MENU_ELEGANT(TemplateType.MENU_ORDERING, false),
    /** Warm, photography-led cafe/bistro style: bold headline hero, real combo-box deals, card-grid menu with sticky category filters. */
    MENU_BISTRO(TemplateType.MENU_ORDERING, false),
    /** Full-bleed dark hero, centered content, services grid, work gallery. */
    PORTFOLIO_HERO(TemplateType.PORTFOLIO, true),
    /** Light editorial split-screen - fixed left profile panel, scrollable right content. */
    PORTFOLIO_MINIMAL(TemplateType.PORTFOLIO, true),
    /** Vibrant creative-agency style - bold typography, asymmetric accents, masonry work gallery as the centerpiece. */
    PORTFOLIO_BOLD(TemplateType.PORTFOLIO, true),
    /** Personal, photo-led homepage - real profile photo hero with a floating highlight badge, a featured-projects grid. */
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
