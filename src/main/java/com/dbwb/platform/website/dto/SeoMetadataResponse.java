package com.dbwb.platform.website.dto;

import com.dbwb.platform.website.entity.SeoMetadata;

public record SeoMetadataResponse(String metaTitle, String metaDescription, String ogImageUrl) {

    public static SeoMetadataResponse empty() {
        return new SeoMetadataResponse(null, null, null);
    }

    public static SeoMetadataResponse from(SeoMetadata seo) {
        return new SeoMetadataResponse(seo.getMetaTitle(), seo.getMetaDescription(), seo.getOgImageUrl());
    }
}
