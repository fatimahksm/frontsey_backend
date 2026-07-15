package com.dbwb.platform.gallery;

import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.gallery.entity.GalleryImage;
import com.dbwb.platform.gallery.repository.GalleryImageRepository;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.subscription.SubscriptionQueryService;
import com.dbwb.platform.website.WebsiteAccessGuard;
import com.dbwb.platform.website.entity.BusinessWebsite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * BRD 9.18: gallery management. Accepts an already-hosted image URL rather
 * than a file upload - actual file storage/processing (BR-DATA-002/003,
 * automatic compression/optimization) needs an image-processing provider
 * decision that hasn't been made yet (TBD-008). Plan-based storage limits
 * (BR-DATA-001) are enforced here by image count against Plan.maxGalleryImages
 * as the closest available proxy until an actual storage-size provider exists.
 */
@Service
public class GalleryService {

    private final GalleryImageRepository repository;
    private final WebsiteAccessGuard accessGuard;
    private final SubscriptionQueryService subscriptionQueryService;

    public GalleryService(
            GalleryImageRepository repository,
            WebsiteAccessGuard accessGuard,
            SubscriptionQueryService subscriptionQueryService) {
        this.repository = repository;
        this.accessGuard = accessGuard;
        this.subscriptionQueryService = subscriptionQueryService;
    }

    @Transactional(readOnly = true)
    public List<GalleryImage> list(UUID websiteId, AuthenticatedAccount caller) {
        accessGuard.requireReadAccess(websiteId, caller);
        return repository.findByWebsiteIdOrderBySortOrder(websiteId);
    }

    @Transactional
    public GalleryImage add(UUID websiteId, AuthenticatedAccount caller, String imageUrl) {
        BusinessWebsite website = accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);

        List<GalleryImage> existing = repository.findByWebsiteIdOrderBySortOrder(websiteId);
        int maxImages = subscriptionQueryService.getActivePlan(websiteId)
                .map(plan -> plan.getMaxGalleryImages())
                .orElse(0);
        if (existing.size() >= maxImages) {
            throw new BusinessRuleViolationException(
                    "This plan allows a maximum of " + maxImages + " gallery images.");
        }

        GalleryImage image = new GalleryImage();
        image.setWebsite(website);
        image.setImageUrl(imageUrl);
        image.setSortOrder(existing.size());
        image.setCover(existing.isEmpty()); // BR-PROF-003: the first image becomes the cover by default
        return repository.save(image);
    }

    @Transactional
    public void delete(UUID websiteId, UUID imageId, AuthenticatedAccount caller) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        GalleryImage image = load(imageId, websiteId);
        boolean wasCover = image.isCover();
        repository.delete(image);

        if (wasCover) {
            repository.findByWebsiteIdOrderBySortOrder(websiteId).stream()
                    .findFirst()
                    .ifPresent(next -> next.setCover(true));
        }
    }

    @Transactional
    public void setCover(UUID websiteId, UUID imageId, AuthenticatedAccount caller) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        GalleryImage target = load(imageId, websiteId);
        repository.findByWebsiteIdOrderBySortOrder(websiteId).forEach(img -> img.setCover(img.getId().equals(target.getId())));
    }

    /** BR-PROF-003: reorders the whole gallery - imageIds must be the complete, ordered set for this website. */
    @Transactional
    public void reorder(UUID websiteId, AuthenticatedAccount caller, List<UUID> imageIds) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        for (int i = 0; i < imageIds.size(); i++) {
            load(imageIds.get(i), websiteId).setSortOrder(i);
        }
    }

    private GalleryImage load(UUID imageId, UUID websiteId) {
        GalleryImage image = repository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery image not found."));
        if (!image.getWebsite().getId().equals(websiteId)) {
            throw new ResourceNotFoundException("Gallery image not found.");
        }
        return image;
    }
}
