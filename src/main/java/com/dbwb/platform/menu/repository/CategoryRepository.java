package com.dbwb.platform.menu.repository;

import com.dbwb.platform.menu.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByWebsiteId(UUID websiteId);
    long countByWebsiteId(UUID websiteId);
}
