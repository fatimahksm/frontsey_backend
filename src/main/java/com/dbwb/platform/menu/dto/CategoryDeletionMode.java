package com.dbwb.platform.menu.dto;

/** BR-MENU-012: deleting a category with items requires an explicit Owner choice. */
public enum CategoryDeletionMode {
    MOVE_ITEMS_TO_CATEGORY,
    DELETE_ITEMS,
    CANCEL
}
