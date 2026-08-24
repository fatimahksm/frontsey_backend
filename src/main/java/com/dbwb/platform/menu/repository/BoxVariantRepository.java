package com.dbwb.platform.menu.repository;

import com.dbwb.platform.menu.entity.BoxVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BoxVariantRepository extends JpaRepository<BoxVariant, UUID> {
    List<BoxVariant> findByMenuItemId(UUID menuItemId);

    /** Every box variant for a whole page of items in one query - see PublicWebsiteService. */
    List<BoxVariant> findByMenuItemIdIn(Collection<UUID> menuItemIds);
}
