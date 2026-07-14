package com.dbwb.platform.profile.repository;

import com.dbwb.platform.profile.entity.BusinessProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, UUID> {
    Optional<BusinessProfile> findByWebsiteId(UUID websiteId);
}
