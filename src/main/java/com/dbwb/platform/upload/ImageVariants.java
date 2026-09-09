package com.dbwb.platform.upload;

/**
 * How a small copy of an uploaded image is named, and the only place that
 * naming is decided.
 *
 * The problem it solves, measured: a photograph off a phone is stored at
 * 2000px and about 600KB. A shop's catalogue shows it as a 48px thumbnail, and
 * a page of thirty rows was therefore downloading roughly 17MB to draw 30
 * postage stamps. At 400px the same page is about 1.3MB.
 *
 * The small copy is made by the browser at upload time, not here and not on
 * read. That matters for where the bytes are served from: with R2 the public
 * URL points at Cloudflare's own network and no server of ours is in the path,
 * so a resize-on-read endpoint would drag every image request back through the
 * application - the opposite of what a shop under load needs. Two objects
 * uploaded once are served by the CDN forever.
 *
 * There is no thumbnail column anywhere, on purpose: an image URL is stored in
 * a dozen places (menu items, logos, covers, galleries, projects, events) and
 * adding a second column to each is a dozen migrations for one number. The
 * variant's key is derived from the original's instead. The marker in the key
 * is what makes that safe - it says this upload has a small copy, so a client
 * never asks for one that was never made and no old image turns into a 404.
 */
public final class ImageVariants {

    /** Sits just before the extension of an original that has a small copy. */
    public static final String ORIGINAL_MARKER = "~v";

    /** Replaces the marker in the small copy's own key. */
    public static final String THUMBNAIL_MARKER = "~400";

    /** Longest edge of the small copy, in pixels. Enough for a 2x product tile on a phone. */
    public static final int THUMBNAIL_EDGE = 400;

    private ImageVariants() {
    }

    /** `abc~v.jpg` -> `abc~400.jpg`. Anything without the marker is returned untouched. */
    public static String thumbnailKey(String originalKey) {
        int marker = originalKey.lastIndexOf(ORIGINAL_MARKER + ".");
        if (marker < 0) return originalKey;
        return originalKey.substring(0, marker) + THUMBNAIL_MARKER + originalKey.substring(marker + ORIGINAL_MARKER.length());
    }

    public static boolean hasThumbnail(String key) {
        return key != null && key.contains(ORIGINAL_MARKER + ".");
    }
}
