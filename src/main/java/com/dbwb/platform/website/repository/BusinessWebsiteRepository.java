package com.dbwb.platform.website.repository;

import com.dbwb.platform.website.entity.BusinessWebsite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessWebsiteRepository extends JpaRepository<BusinessWebsite, UUID> {
    Optional<BusinessWebsite> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<BusinessWebsite> findByOwnerId(UUID ownerId);
    long countByOwnerId(UUID ownerId);
}
