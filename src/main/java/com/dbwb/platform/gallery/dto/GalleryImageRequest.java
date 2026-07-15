package com.dbwb.platform.gallery.dto;

import jakarta.validation.constraints.NotBlank;

/** The image itself is assumed already uploaded/hosted elsewhere - see GalleryService for why (TBD-008). */
public record GalleryImageRequest(@NotBlank String imageUrl) {
}
