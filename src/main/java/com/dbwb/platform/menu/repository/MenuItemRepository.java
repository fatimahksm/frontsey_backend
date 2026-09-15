package com.dbwb.platform.menu.repository;

import com.dbwb.platform.menu.entity.ItemAvailability;
import com.dbwb.platform.menu.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findByWebsiteIdAndTrashedAtIsNull(UUID websiteId);

    // The paged forms, for the owner's item list. A shop with five hundred
    // lines sent all five hundred to draw one screen of them, and three other
    // callers fetched the same five hundred only to count them.
    Page<MenuItem> findByWebsiteIdAndTrashedAtIsNull(UUID websiteId, Pageable pageable);

    Page<MenuItem> findByWebsiteIdAndCategoryIdAndTrashedAtIsNull(UUID websiteId, UUID categoryId, Pageable pageable);

    Page<MenuItem> findByWebsiteIdAndNameContainingIgnoreCaseAndTrashedAtIsNull(
            UUID websiteId, String name, Pageable pageable);

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
