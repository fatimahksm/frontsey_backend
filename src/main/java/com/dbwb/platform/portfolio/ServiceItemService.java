package com.dbwb.platform.portfolio;

import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.portfolio.dto.ServiceItemRequest;
import com.dbwb.platform.portfolio.entity.ServiceItem;
import com.dbwb.platform.portfolio.repository.ServiceItemRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.website.WebsiteAccessGuard;
import com.dbwb.platform.website.entity.BusinessWebsite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * The PORTFOLIO-template counterpart to MenuService: services shown on a
 * portfolio website. Reuses Permission.MANAGE_MENU for Manager access
 * control rather than adding a new permission - conceptually it's the same
 * "manage this website's sellable content" grant, just applied to whichever
 * content model this website's TemplateType actually uses.
 */
@Service
public class ServiceItemService {

    private final WebsiteAccessGuard accessGuard;
    private final ServiceItemRepository repository;

    public ServiceItemService(WebsiteAccessGuard accessGuard, ServiceItemRepository repository) {
        this.accessGuard = accessGuard;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ServiceItem> list(UUID websiteId, AuthenticatedAccount caller) {
        accessGuard.requireReadAccess(websiteId, caller);
        return repository.findByWebsiteIdOrderBySortOrder(websiteId);
    }

    @Transactional
    public ServiceItem create(UUID websiteId, AuthenticatedAccount caller, ServiceItemRequest request) {
        BusinessWebsite website = accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);

        ServiceItem service = new ServiceItem();
        service.setWebsite(website);
        service.setSortOrder((int) repository.countByWebsiteId(websiteId));
        applyRequest(service, request);
        return repository.save(service);
    }

    @Transactional
    public ServiceItem update(UUID websiteId, UUID serviceId, AuthenticatedAccount caller, ServiceItemRequest request) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        ServiceItem service = load(serviceId, websiteId);
        applyRequest(service, request);
        return service;
    }

    @Transactional
    public void delete(UUID websiteId, UUID serviceId, AuthenticatedAccount caller) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        repository.delete(load(serviceId, websiteId));
    }

    /** Reorders the whole list - serviceIds must be the complete, ordered set for this website. */
    @Transactional
    public void reorder(UUID websiteId, AuthenticatedAccount caller, List<UUID> serviceIds) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        for (int i = 0; i < serviceIds.size(); i++) {
            load(serviceIds.get(i), websiteId).setSortOrder(i);
        }
    }

    private void applyRequest(ServiceItem service, ServiceItemRequest request) {
        service.setName(request.name());
        service.setDescription(request.description());
        service.setPrice(request.price());
        service.setImageUrl(request.imageUrl());
    }

    private ServiceItem load(UUID serviceId, UUID websiteId) {
        ServiceItem service = repository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found."));
        if (!service.getWebsite().getId().equals(websiteId)) {
            throw new ResourceNotFoundException("Service not found.");
        }
        return service;
    }
}
