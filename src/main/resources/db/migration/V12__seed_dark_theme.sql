-- A dark preset, so the Classic display-only menu can be given the
-- dark-background / warm-accent look without an owner hand-editing colors.
-- Same ThemeConfig schema as V9 (ThemeConfigValidator enforces it); the
-- Classic layout paints itself entirely from these values, so background,
-- surface and text here really do drive the published page.

INSERT INTO themes (id, name, description, theme_config, active, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'Midnight Gold', 'A dark, photo-led look with a warm gold accent - built for menus.',
     '{"fontFamily":"MODERN_SANS","headingFontFamily":"CLASSIC_SERIF","primaryColor":"#f5b921","secondaryColor":"#1c1c1c","backgroundColor":"#0b0b0b","surfaceColor":"#161616","textColor":"#f5f5f5","buttonStyle":"ROUNDED","cardStyle":"FLAT","borderRadius":10,"sectionSpacing":"COMFORTABLE"}',
     TRUE, now(), now());
