package com.dbwb.platform.portfolio.repository;

import com.dbwb.platform.portfolio.entity.PortfolioProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PortfolioProjectRepository extends JpaRepository<PortfolioProject, UUID> {
    List<PortfolioProject> findByWebsiteIdOrderBySortOrder(UUID websiteId);
}
