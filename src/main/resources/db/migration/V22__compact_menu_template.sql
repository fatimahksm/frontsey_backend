-- The compact digital menu: no photographs, a Food/Beverages switch, and dense
-- name-and-price rows. The kind of menu a customer scans at a table.
--
-- Priced and active from the start, unlike the events template: this one was
-- asked for as a template to use, not one to hold back until it is switched on.
INSERT INTO template_prices (id, layout_variant, monthly_price, yearly_price, plan_code, active, created_at, updated_at)
VALUES (gen_random_uuid(), 'MENU_COMPACT', 9.99, 99.99, 'BASIC', TRUE, now(), now())
ON CONFLICT (layout_variant) DO NOTHING;
