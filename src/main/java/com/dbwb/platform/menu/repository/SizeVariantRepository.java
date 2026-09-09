package com.dbwb.platform.menu.repository;

import com.dbwb.platform.menu.entity.SizeVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SizeVariantRepository extends JpaRepository<SizeVariant, UUID> {
    List<SizeVariant> findByMenuItemId(UUID menuItemId);

    /** Every size for a whole page of items in one query - see PublicWebsiteService. */
    List<SizeVariant> findByMenuItemIdIn(Collection<UUID> menuItemIds);
}
