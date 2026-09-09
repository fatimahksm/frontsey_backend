-- Indexes on the foreign keys the public page reads on every visit.
--
-- Postgres indexes a primary key automatically but never a foreign key, and
-- these four were declared as REFERENCES in V1 with no index of their own.
-- PublicWebsiteService loads a menu with "WHERE menu_item_id IN (...)" against
-- three of them and "WHERE addon_group_id IN (...)" against the fourth, so
-- every public page load was a sequential scan.
--
-- Measured on a seeded platform of 500 businesses and 20,000 items: fetching
-- the sizes for one 40-item menu read 19,960 rows to return 40. The cost grows
-- with the size of the whole platform rather than with the menu being viewed,
-- which is the shape that stops a multi-tenant product scaling at all. With
-- the index the same query touches one heap block.
--
-- CONCURRENTLY is deliberately not used: Flyway runs migrations in a
-- transaction and CREATE INDEX CONCURRENTLY cannot run inside one. These
-- tables are small enough that a brief lock at deploy time is the cheaper
-- trade; if that stops being true, build them by hand out of band instead.
CREATE INDEX IF NOT EXISTS idx_menu_item_sizes_item ON menu_item_sizes(menu_item_id);
CREATE INDEX IF NOT EXISTS idx_menu_item_addon_groups_item ON menu_item_addon_groups(menu_item_id);
CREATE INDEX IF NOT EXISTS idx_menu_item_addons_group ON menu_item_addons(addon_group_id);
CREATE INDEX IF NOT EXISTS idx_menu_item_box_variants_item ON menu_item_box_variants(menu_item_id);

-- Only these four. Everything else on the public path is already covered:
-- services has idx_services_website, and business_profiles, subscriptions and
-- seo_metadata declare website_id UNIQUE, which builds an index on its own.
-- A second index there would cost writes and disk for nothing.
