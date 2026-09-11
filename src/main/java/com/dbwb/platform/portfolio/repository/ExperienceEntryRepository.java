package com.dbwb.platform.portfolio.repository;

import com.dbwb.platform.portfolio.entity.ExperienceEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExperienceEntryRepository extends JpaRepository<ExperienceEntry, UUID> {
    List<ExperienceEntry> findByWebsiteIdOrderBySortOrder(UUID websiteId);
}
