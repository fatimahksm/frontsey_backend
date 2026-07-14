package com.dbwb.platform.delivery.repository;

import com.dbwb.platform.delivery.entity.DeliveryArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryAreaRepository extends JpaRepository<DeliveryArea, UUID> {
    List<DeliveryArea> findByWebsiteId(UUID websiteId);
}
