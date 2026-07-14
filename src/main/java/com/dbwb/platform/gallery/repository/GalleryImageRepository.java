package com.dbwb.platform.gallery.repository;

import com.dbwb.platform.gallery.entity.GalleryImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GalleryImageRepository extends JpaRepository<GalleryImage, UUID> {
    List<GalleryImage> findByWebsiteIdOrderBySortOrder(UUID websiteId);
}
