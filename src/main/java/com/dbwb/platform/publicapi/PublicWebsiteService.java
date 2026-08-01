package com.dbwb.platform.publicapi;

import com.dbwb.platform.delivery.repository.DeliveryAreaRepository;
import com.dbwb.platform.gallery.repository.GalleryImageRepository;
import com.dbwb.platform.menu.entity.Addon;
import com.dbwb.platform.menu.entity.AddonGroup;
import com.dbwb.platform.menu.repository.AddonGroupRepository;
import com.dbwb.platform.menu.repository.AddonRepository;
import com.dbwb.platform.menu.repository.BoxVariantRepository;
import com.dbwb.platform.menu.repository.CategoryRepository;
import com.dbwb.platform.menu.repository.MenuItemRepository;
import com.dbwb.platform.menu.repository.SizeVariantRepository;
import com.dbwb.platform.portfolio.repository.ServiceItemRepository;
import com.dbwb.platform.profile.repository.BusinessProfileRepository;
import com.dbwb.platform.sections.repository.PageSectionRepository;
import com.dbwb.platform.profile.repository.OpeningHoursRepository;
import com.dbwb.platform.publicapi.dto.PublicMenuItem;
import com.dbwb.platform.publicapi.dto.PublicWebsiteResponse;
import com.dbwb.platform.theme.ThemeConfigValidator;
import com.dbwb.platform.theme.dto.ThemeConfig;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.WebsiteStatus;
import com.dbwb.platform.website.repository.BusinessWebsiteRepository;
import com.dbwb.platform.website.repository.SeoMetadataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * BR-QR-003/004: an unknown/deleted/trashed slug yields NOT_FOUND (branded
 * 404); a suspended website yields UNAVAILABLE with no internal reason
 * exposed; only PUBLISHED (or EXPIRED-but-still-within-grace, handled by the
 * subscription module flipping status) returns the full payload.
 */
@Service
public class PublicWebsiteService {

    private final BusinessWebsiteRepository websiteRepository;
    private final BusinessProfileRepository profileRepository;
    private final OpeningHoursRepository openingHoursRepository;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final SizeVariantRepository sizeVariantRepository;
    private final AddonGroupRepository addonGroupRepository;
    private final AddonRepository addonRepository;
    private final BoxVariantRepository boxVariantRepository;
    private final DeliveryAreaRepository deliveryAreaRepository;
    private final GalleryImageRepository galleryImageRepository;
    private final SeoMetadataRepository seoMetadataRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final PageSectionRepository pageSectionRepository;
    private final ThemeConfigValidator themeConfigValidator;

    public PublicWebsiteService(
            BusinessWebsiteRepository websiteRepository,
            BusinessProfileRepository profileRepository,
            OpeningHoursRepository openingHoursRepository,
            CategoryRepository categoryRepository,
            MenuItemRepository menuItemRepository,
            SizeVariantRepository sizeVariantRepository,
            AddonGroupRepository addonGroupRepository,
            AddonRepository addonRepository,
            BoxVariantRepository boxVariantRepository,
            DeliveryAreaRepository deliveryAreaRepository,
            GalleryImageRepository galleryImageRepository,
            SeoMetadataRepository seoMetadataRepository,
            ServiceItemRepository serviceItemRepository,
            PageSectionRepository pageSectionRepository,
            ThemeConfigValidator themeConfigValidator) {
        this.websiteRepository = websiteRepository;
        this.profileRepository = profileRepository;
        this.openingHoursRepository = openingHoursRepository;
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.sizeVariantRepository = sizeVariantRepository;
        this.addonGroupRepository = addonGroupRepository;
        this.addonRepository = addonRepository;
        this.boxVariantRepository = boxVariantRepository;
        this.deliveryAreaRepository = deliveryAreaRepository;
        this.galleryImageRepository = galleryImageRepository;
        this.seoMetadataRepository = seoMetadataRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.pageSectionRepository = pageSectionRepository;
        this.themeConfigValidator = themeConfigValidator;
    }

    @Transactional(readOnly = true)
    public Optional<UUID> findWebsiteIdBySlug(String slug) {
        return websiteRepository.findBySlug(slug).map(BusinessWebsite::getId);
    }

    @Transactional(readOnly = true)
    public PublicWebsiteEnvelope getBySlug(String slug) {
        BusinessWebsite website = websiteRepository.findBySlug(slug).orElse(null);
        if (website == null) {
            return PublicWebsiteEnvelope.notFound();
        }

        return switch (website.getStatus()) {
            case PUBLISHED -> PublicWebsiteEnvelope.available(assemble(website, website.getPublishedContent()));
            case SUSPENDED_TEMPORARY, SUSPENDED_PERMANENT, EXPIRED -> PublicWebsiteEnvelope.unavailable();
            case DRAFT, TRASHED, DELETED -> PublicWebsiteEnvelope.notFound();
        };
    }

    /** Owner/manager preview of the current draft, regardless of publish status - access is gated by the caller (see WebsiteAccessGuard at the call site). */
    @Transactional(readOnly = true)
    public PublicWebsiteResponse assembleForPreview(BusinessWebsite website) {
        return assemble(website, website.getDraftContent());
    }

    private PublicWebsiteResponse assemble(BusinessWebsite website, String content) {
        var profileEntity = profileRepository.findByWebsiteId(website.getId()).orElse(null);
        PublicWebsiteResponse.PublicProfile profile = null;
        if (profileEntity != null) {
            Map<String, String> policies = new HashMap<>();
            if (profileEntity.isShowPrivacyPolicy()) policies.put("PRIVACY", profileEntity.getPrivacyPolicyContent());
            if (profileEntity.isShowTermsAndConditions()) policies.put("TERMS", profileEntity.getTermsAndConditionsContent());
            if (profileEntity.isShowDeliveryPolicy()) policies.put("DELIVERY", profileEntity.getDeliveryPolicyContent());
            if (profileEntity.isShowRefundPolicy()) policies.put("REFUND", profileEntity.getRefundPolicyContent());

            profile = new PublicWebsiteResponse.PublicProfile(
                    profileEntity.getDescription(), profileEntity.getLogoUrl(), profileEntity.getCoverImageUrl(),
                    profileEntity.getPhone(), profileEntity.getWhatsappNumber(), profileEntity.getEmail(),
                    profileEntity.getAddress(), profileEntity.getGoogleMapsUrl(), profileEntity.getInstagramUrl(),
                    profileEntity.getTiktokUrl(), policies);
        }

        List<PublicWebsiteResponse.PublicOpeningHours> hours = openingHoursRepository
                .findByWebsiteIdOrderByDayOfWeek(website.getId()).stream()
                .map(h -> new PublicWebsiteResponse.PublicOpeningHours(
                        h.getDayOfWeek().name(), h.isOpen(),
                        h.getOpensAt() == null ? null : h.getOpensAt().toString(),
                        h.getClosesAt() == null ? null : h.getClosesAt().toString()))
                .toList();

        List<PublicWebsiteResponse.PublicCategory> categories = categoryRepository.findByWebsiteId(website.getId())
                .stream()
                .map(category -> {
                    var items = menuItemRepository
                            .findByWebsiteIdAndCategoryIdAndTrashedAtIsNull(website.getId(), category.getId())
                            .stream()
                            .map(item -> {
                                var sizes = sizeVariantRepository.findByMenuItemId(item.getId());
                                var groups = addonGroupRepository.findByMenuItemId(item.getId());
                                Map<java.util.UUID, List<Addon>> addonsByGroup = new HashMap<>();
                                for (AddonGroup g : groups) {
                                    addonsByGroup.put(g.getId(), addonRepository.findByAddonGroupId(g.getId()));
                                }
                                var boxVariants = boxVariantRepository.findByMenuItemId(item.getId());
                                return PublicMenuItem.from(item, sizes, groups, addonsByGroup, boxVariants);
                            })
                            .toList();
                    return new PublicWebsiteResponse.PublicCategory(category.getId().toString(), category.getName(), items);
                })
                .toList();

        List<PublicWebsiteResponse.PublicDeliveryArea> areas = deliveryAreaRepository
                .findByWebsiteId(website.getId()).stream()
                .map(a -> new PublicWebsiteResponse.PublicDeliveryArea(
                        a.getId().toString(), a.getName(), a.getDeliveryFee(),
                        a.getMinimumOrderAmount(), a.getFreeDeliveryThreshold()))
                .toList();

        List<String> galleryUrls = galleryImageRepository.findByWebsiteIdOrderBySortOrder(website.getId())
                .stream().map(com.dbwb.platform.gallery.entity.GalleryImage::getImageUrl).toList();

        PublicWebsiteResponse.PublicSeoMetadata seo = seoMetadataRepository.findByWebsiteId(website.getId())
                .map(s -> new PublicWebsiteResponse.PublicSeoMetadata(s.getMetaTitle(), s.getMetaDescription(), s.getOgImageUrl()))
                .orElse(new PublicWebsiteResponse.PublicSeoMetadata(website.getBusinessName(), null, null));

        List<PublicWebsiteResponse.PublicService> services = serviceItemRepository
                .findByWebsiteIdOrderBySortOrder(website.getId()).stream()
                .map(s -> new PublicWebsiteResponse.PublicService(
                        s.getId().toString(), s.getName(), s.getDescription(), s.getPrice(), s.getImageUrl()))
                .toList();

        List<PublicWebsiteResponse.PublicPageSection> sections = pageSectionRepository
                .findByWebsiteIdOrderBySortOrder(website.getId()).stream()
                .map(s -> new PublicWebsiteResponse.PublicPageSection(s.getId().toString(), s.getType(), s.getData()))
                .toList();

        // Phase 3: every website has an *effective* theme, whether it picked a preset or is building from
        // scratch - re-validated defensively here since the column is still free-text TEXT at the DB level.
        ThemeConfig theme = website.getTheme() != null
                ? themeConfigValidator.parseOrDefault(website.getTheme().getThemeConfig())
                : ThemeConfig.defaults();

        return new PublicWebsiteResponse(
                website.getBusinessName(), website.getSlug(), website.getPageMode(), website.getTemplateType(),
                website.getEffectiveLayoutVariant(), website.getOrderingMode(), website.getPrimaryLanguage(), website.getCurrency(),
                content, profile, hours, categories, areas, services, galleryUrls, seo, sections, theme);
    }
}
