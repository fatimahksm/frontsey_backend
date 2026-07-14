package com.dbwb.platform.manager.entity;

/**
 * BR-MGR-003: permissions are assigned individually, not through fixed
 * manager role bundles. This enum is the closed set of grantable actions.
 */
public enum Permission {
    MANAGE_MENU,
    MANAGE_PRICES,
    MANAGE_THEME_AND_CONTENT,
    VIEW_ANALYTICS,
    PUBLISH_WEBSITE,
    MANAGE_BUSINESS_PROFILE,
    MANAGE_DELIVERY_SETTINGS
}
