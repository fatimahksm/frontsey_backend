-- Phase 3: seeds a few starter themes so the theme picker isn't empty and
-- there's real, working theme data to switch between. Each themeConfig
-- value conforms to com.dbwb.platform.theme.dto.ThemeConfig - the same
-- schema ThemeConfigValidator enforces on every future admin create/update.

INSERT INTO themes (id, name, description, theme_config, active, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'Classic Violet', 'The default Frontsey look - clean, modern, and neutral.',
     '{"fontFamily":"SYSTEM_SANS","headingFontFamily":"SYSTEM_SANS","primaryColor":"#7c3aed","secondaryColor":"#f4f0fb","backgroundColor":"#ffffff","surfaceColor":"#ffffff","textColor":"#0a0a0f","buttonStyle":"PILL","cardStyle":"SOFT_SHADOW","borderRadius":16,"sectionSpacing":"COMFORTABLE"}',
     TRUE, now(), now()),
    (gen_random_uuid(), 'Warm Amber', 'A warm, inviting palette for cafes and restaurants.',
     '{"fontFamily":"MODERN_SANS","headingFontFamily":"ELEGANT_SERIF","primaryColor":"#e67e22","secondaryColor":"#fff4ea","backgroundColor":"#ffffff","surfaceColor":"#fafafa","textColor":"#171717","buttonStyle":"ROUNDED","cardStyle":"SOFT_SHADOW","borderRadius":16,"sectionSpacing":"COMFORTABLE"}',
     TRUE, now(), now()),
    (gen_random_uuid(), 'Minimal Mono', 'A crisp, no-frills look with sharp corners and monospace type.',
     '{"fontFamily":"MONOSPACE","headingFontFamily":"MONOSPACE","primaryColor":"#171717","secondaryColor":"#f0f0f0","backgroundColor":"#ffffff","surfaceColor":"#fafafa","textColor":"#171717","buttonStyle":"SQUARE","cardStyle":"BORDERED","borderRadius":4,"sectionSpacing":"SPACIOUS"}',
     TRUE, now(), now()),
    (gen_random_uuid(), 'Editorial Serif', 'An elegant, editorial style with generous spacing.',
     '{"fontFamily":"CLASSIC_SERIF","headingFontFamily":"ELEGANT_SERIF","primaryColor":"#9c2b2b","secondaryColor":"#f7ece7","backgroundColor":"#fffdf9","surfaceColor":"#ffffff","textColor":"#2b2621","buttonStyle":"ROUNDED","cardStyle":"BORDERED","borderRadius":8,"sectionSpacing":"SPACIOUS"}',
     TRUE, now(), now());
