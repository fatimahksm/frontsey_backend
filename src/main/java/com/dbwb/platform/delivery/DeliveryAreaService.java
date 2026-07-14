package com.dbwb.platform.delivery;

import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.delivery.entity.DeliveryArea;
import com.dbwb.platform.delivery.repository.DeliveryAreaRepository;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.website.WebsiteAccessGuard;
import com.dbwb.platform.website.entity.BusinessWebsite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class DeliveryAreaService {

    private final DeliveryAreaRepository repository;
    private final WebsiteAccessGuard accessGuard;

    public DeliveryAreaService(DeliveryAreaRepository repository, WebsiteAccessGuard accessGuard) {
        this.repository = repository;
        this.accessGuard = accessGuard;
    }

    @Transactional
    public DeliveryArea create(UUID websiteId, AuthenticatedAccount caller, String name,
                                BigDecimal fee, BigDecimal minimumOrder, BigDecimal freeThreshold) {
        BusinessWebsite website = accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_DELIVERY_SETTINGS);
        DeliveryArea area = new DeliveryArea();
        area.setWebsite(website);
        area.setName(name);
        area.setDeliveryFee(fee);
        area.setMinimumOrderAmount(minimumOrder);
        area.setFreeDeliveryThreshold(freeThreshold);
        return repository.save(area);
    }

    @Transactional(readOnly = true)
    public List<DeliveryArea> list(UUID websiteId, AuthenticatedAccount caller) {
        accessGuard.requireReadAccess(websiteId, caller);
        return repository.findByWebsiteId(websiteId);
    }

    @Transactional
    public void delete(UUID websiteId, UUID areaId, AuthenticatedAccount caller) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_DELIVERY_SETTINGS);
        DeliveryArea area = repository.findById(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery area not found."));
        if (!area.getWebsite().getId().equals(websiteId)) {
            throw new ResourceNotFoundException("Delivery area not found.");
        }
        repository.delete(area);
    }
}
