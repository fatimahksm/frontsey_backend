package com.dbwb.platform.menu.repository;

import com.dbwb.platform.menu.entity.BoxVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BoxVariantRepository extends JpaRepository<BoxVariant, UUID> {
    List<BoxVariant> findByMenuItemId(UUID menuItemId);
}
