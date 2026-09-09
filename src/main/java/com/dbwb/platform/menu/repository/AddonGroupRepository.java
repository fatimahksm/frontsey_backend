package com.dbwb.platform.menu.repository;

import com.dbwb.platform.menu.entity.AddonGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AddonGroupRepository extends JpaRepository<AddonGroup, UUID> {
    List<AddonGroup> findByMenuItemId(UUID menuItemId);

    /** Every group for a whole page of items in one query - see PublicWebsiteService. */
    List<AddonGroup> findByMenuItemIdIn(Collection<UUID> menuItemIds);
}
