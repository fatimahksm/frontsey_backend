package com.dbwb.platform.menu.repository;

import com.dbwb.platform.menu.entity.ItemAvailability;
import com.dbwb.platform.menu.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findByWebsiteIdAndTrashedAtIsNull(UUID websiteId);

    /** BR-MENU-011: lets the Owner see what's in the trash to decide what to restore. */
    List<MenuItem> findByWebsiteIdAndTrashedAtIsNotNull(UUID websiteId);

    List<MenuItem> findByWebsiteIdAndCategoryIdAndTrashedAtIsNull(UUID websiteId, UUID categoryId);

    List<MenuItem> findByWebsiteIdAndNameContainingIgnoreCaseAndTrashedAtIsNull(UUID websiteId, String name);

    long countByCategoryIdAndTrashedAtIsNull(UUID categoryId);

    /** BR-MENU-006: items whose temporary-unavailability window has elapsed and must revert automatically. */
    List<MenuItem> findByAvailabilityAndUnavailableUntilBefore(ItemAvailability availability, Instant instant);

    /** BR-IMP-003: used to detect a duplicate item name during menu import. */
    java.util.Optional<MenuItem> findByWebsiteIdAndNameIgnoreCaseAndTrashedAtIsNull(UUID websiteId, String name);
}
