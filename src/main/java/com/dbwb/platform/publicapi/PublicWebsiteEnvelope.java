package com.dbwb.platform.publicapi;

import com.dbwb.platform.publicapi.dto.PublicWebsiteResponse;

public record PublicWebsiteEnvelope(PublicWebsiteStatus status, PublicWebsiteResponse website) {

    public static PublicWebsiteEnvelope notFound() {
        return new PublicWebsiteEnvelope(PublicWebsiteStatus.NOT_FOUND, null);
    }

    public static PublicWebsiteEnvelope unavailable() {
        return new PublicWebsiteEnvelope(PublicWebsiteStatus.UNAVAILABLE, null);
    }

    public static PublicWebsiteEnvelope available(PublicWebsiteResponse website) {
        return new PublicWebsiteEnvelope(PublicWebsiteStatus.AVAILABLE, website);
    }
}
