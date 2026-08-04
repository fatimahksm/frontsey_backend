package com.dbwb.platform.portfolio.repository;

import com.dbwb.platform.portfolio.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, UUID> {
    List<ServiceItem> findByWebsiteIdOrderBySortOrder(UUID websiteId);

    long countByWebsiteId(UUID websiteId);
}
