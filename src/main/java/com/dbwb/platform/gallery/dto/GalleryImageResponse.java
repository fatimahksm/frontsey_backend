package com.dbwb.platform.gallery.dto;

import com.dbwb.platform.gallery.entity.GalleryImage;

import java.util.UUID;

public record GalleryImageResponse(UUID id, String imageUrl, int sortOrder, boolean cover) {
    public static GalleryImageResponse from(GalleryImage image) {
        return new GalleryImageResponse(image.getId(), image.getImageUrl(), image.getSortOrder(), image.isCover());
    }
}
