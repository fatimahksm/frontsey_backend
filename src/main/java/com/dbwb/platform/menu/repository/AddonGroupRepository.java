package com.dbwb.platform.menu.repository;

import com.dbwb.platform.menu.entity.AddonGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AddonGroupRepository extends JpaRepository<AddonGroup, UUID> {
    List<AddonGroup> findByMenuItemId(UUID menuItemId);
}
