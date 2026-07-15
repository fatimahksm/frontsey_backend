package com.dbwb.platform.website.repository;

import com.dbwb.platform.website.entity.SeoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SeoMetadataRepository extends JpaRepository<SeoMetadata, UUID> {
    Optional<SeoMetadata> findByWebsiteId(UUID websiteId);
}
