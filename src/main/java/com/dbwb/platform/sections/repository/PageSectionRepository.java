package com.dbwb.platform.sections.repository;

import com.dbwb.platform.sections.entity.PageSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PageSectionRepository extends JpaRepository<PageSection, UUID> {
    List<PageSection> findByWebsiteIdOrderBySortOrder(UUID websiteId);

    long countByWebsiteId(UUID websiteId);
}
