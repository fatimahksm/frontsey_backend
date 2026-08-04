-- One level of menu sub-categories (Coffee -> Hot / Iced).
-- parent_id NULL = top-level category. The one-level cap (a sub-category can
-- never itself be a parent) is enforced in MenuService, not by the schema,
-- because a self-referencing FK cannot express "depth <= 1" on its own.

ALTER TABLE menu_categories ADD COLUMN parent_id UUID REFERENCES menu_categories(id);

CREATE INDEX idx_menu_categories_parent ON menu_categories(parent_id);
