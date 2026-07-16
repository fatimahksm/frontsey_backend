package com.dbwb.platform.website.entity;

/**
 * A structural page arrangement, orthogonal to both TemplateType (the
 * content model - menu vs. portfolio) and Theme (colors only). Each variant
 * renders the exact same underlying data with a genuinely different visual
 * shape. Owners can preview and switch anytime, same as Theme.
 */
public enum LayoutVariant {
    /** Business-card header (logo/name/description), gallery strip, categorized menu list, sticky cart sidebar. */
    MENU_CLASSIC(TemplateType.MENU_ORDERING),
    /** Full-width cover hero, sticky category tabs, items as a card grid, cart as a slide-out drawer. */
    MENU_GRID(TemplateType.MENU_ORDERING),
    /** Full-bleed dark hero, centered content, services grid, work gallery. */
    PORTFOLIO_HERO(TemplateType.PORTFOLIO),
    /** Light editorial split-screen - fixed left profile panel, scrollable right content. */
    PORTFOLIO_MINIMAL(TemplateType.PORTFOLIO);

    private final TemplateType templateType;

    LayoutVariant(TemplateType templateType) {
        this.templateType = templateType;
    }

    public TemplateType templateType() {
        return templateType;
    }

    public static LayoutVariant defaultFor(TemplateType templateType) {
        return templateType == TemplateType.PORTFOLIO ? PORTFOLIO_HERO : MENU_CLASSIC;
    }
}
