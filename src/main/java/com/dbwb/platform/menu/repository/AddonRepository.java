package com.dbwb.platform.menu.repository;

import com.dbwb.platform.menu.entity.Addon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AddonRepository extends JpaRepository<Addon, UUID> {
    List<Addon> findByAddonGroupId(UUID addonGroupId);

    /** Every add-on for a whole page of groups in one query - see PublicWebsiteService. */
    List<Addon> findByAddonGroupIdIn(Collection<UUID> addonGroupIds);
}
