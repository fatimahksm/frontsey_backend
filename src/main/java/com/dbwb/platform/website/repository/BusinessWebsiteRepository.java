package com.dbwb.platform.website.repository;

import com.dbwb.platform.website.entity.BusinessWebsite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessWebsiteRepository extends JpaRepository<BusinessWebsite, UUID> {
    Optional<BusinessWebsite> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<BusinessWebsite> findByOwnerId(UUID ownerId);
    long countByOwnerId(UUID ownerId);

    /** Eagerly loads the owner association - used wherever the owner is read after the transaction ends (e.g. DTO mapping in a controller). */
    @Query("select w from BusinessWebsite w join fetch w.owner")
    List<BusinessWebsite> findAllWithOwner();
}
