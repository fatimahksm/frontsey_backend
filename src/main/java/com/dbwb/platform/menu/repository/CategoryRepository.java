package com.dbwb.platform.menu.repository;

import com.dbwb.platform.menu.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByWebsiteId(UUID websiteId);
    long countByWebsiteId(UUID websiteId);
    Optional<Category> findByWebsiteIdAndNameIgnoreCase(UUID websiteId, String name);

    /** Top-level categories only - the roots of the one-level category tree. */
    List<Category> findByWebsiteIdAndParentIsNull(UUID websiteId);

    /** The sub-categories directly under one parent. */
    List<Category> findByParentId(UUID parentId);
}
