package com.dbwb.platform.website.dto;

import jakarta.validation.constraints.Size;

public record SeoMetadataRequest(
        @Size(max = 70) String metaTitle,
        @Size(max = 160) String metaDescription,
        String ogImageUrl
) {
}
