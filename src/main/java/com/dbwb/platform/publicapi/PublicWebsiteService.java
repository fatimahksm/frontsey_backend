package com.dbwb.platform.publicapi;

import com.dbwb.platform.common.config.CacheConfig;
import com.dbwb.platform.delivery.repository.DeliveryAreaRepository;
import com.dbwb.platform.events.repository.EventDetailsRepository;
import com.dbwb.platform.events.repository.EventScheduleEntryRepository;
import com.dbwb.platform.gallery.repository.GalleryImageRepository;
import com.dbwb.platform.menu.entity.Addon;
import com.dbwb.platform.menu.entity.AddonGroup;
import com.dbwb.platform.menu.entity.BoxVariant;
import com.dbwb.platform.menu.entity.Category;
import com.dbwb.platform.menu.entity.MenuItem;
import com.dbwb.platform.menu.entity.SizeVariant;
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
import com.dbwb.platform.portfolio.dto.PortfolioProjectResponse;
import com.dbwb.platform.portfolio.repository.PortfolioProjectRepository;
import com.dbwb.platform.theme.dto.ThemeConfig;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.WebsiteStatus;
import com.dbwb.platform.website.repository.BusinessWebsiteRepository;
import com.dbwb.platform.website.repository.SeoMetadataRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final PortfolioProjectRepository portfolioProjectRepository;
    private final EventDetailsRepository eventDetailsRepository;
    private final EventScheduleEntryRepository eventScheduleEntryRepository;

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
            ThemeConfigValidator themeConfigValidator,
            PortfolioProjectRepository portfolioProjectRepository,
            EventDetailsRepository eventDetailsRepository,
            EventScheduleEntryRepository eventScheduleEntryRepository) {
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
        this.portfolioProjectRepository = portfolioProjectRepository;
        this.eventDetailsRepository = eventDetailsRepository;
        this.eventScheduleEntryRepository = eventScheduleEntryRepository;
    }

    @Transactional(readOnly = true)
    public Optional<UUID> findWebsiteIdBySlug(String slug) {
        return websiteRepository.findBySlug(slug).map(BusinessWebsite::getId);
    }

    /**
     * Resolves a slug once and hands back both the payload and the website's
     * id, so a caller that needs to record the visit does not look the same
     * slug up a second time.
     *
     * Cached for a few seconds - see CacheConfig for why by expiry rather than
     * by eviction on every write. A miss is exactly the work this always did;
     * a hit skips the dozen queries and the JSON assembly entirely.
     *
     * Recording the visit is deliberately not in here. It happens in the
     * controller, so a cached page still counts as a page view - a visit that
     * hit a warm cache is still a visit, and analytics that only counted cache
     * misses would quietly undercount by whatever the hit rate happens to be.
     */
    @Cacheable(CacheConfig.PUBLIC_WEBSITES)
    @Transactional(readOnly = true)
    public PublicWebsiteLookup lookupBySlug(String slug) {
        BusinessWebsite website = websiteRepository.findBySlug(slug).orElse(null);
        if (website == null) {
            return new PublicWebsiteLookup(null, PublicWebsiteEnvelope.notFound());
        }

        PublicWebsiteEnvelope envelope = switch (website.getStatus()) {
            case PUBLISHED -> PublicWebsiteEnvelope.available(assemble(website, website.getPublishedContent()));
            case SUSPENDED_TEMPORARY, SUSPENDED_PERMANENT, EXPIRED -> PublicWebsiteEnvelope.unavailable();
            case DRAFT, TRASHED, DELETED -> PublicWebsiteEnvelope.notFound();
        };
        return new PublicWebsiteLookup(website.getId(), envelope);
    }

    /**
     * The payload alone, for callers with no visit to attribute.
     *
     * Deliberately not annotated: this calls lookupBySlug on itself, and a
     * self-call does not pass through the caching proxy, so annotating it
     * would advertise a cache that never gets consulted. Anything on the hot
     * path should call lookupBySlug directly.
     */
    @Transactional(readOnly = true)
    public PublicWebsiteEnvelope getBySlug(String slug) {
        return lookupBySlug(slug).envelope();
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

        // Two-level tree: top-level categories carry their own items plus their
        // sub-categories. Items filed directly against a parent still show, so
        // a menu can mix "Coffee -> Hot/Iced" with un-grouped items.
        //
        // The whole menu is loaded in a fixed number of queries and assembled in
        // memory. Doing it per category and per item instead meant a category
        // query per parent plus four queries for every single item (sizes,
        // add-on groups, add-ons per group, box variants) - a 100-item menu ran
        // over 400 queries on every public page load, against a 3-second target
        // (BR-NFR-001). This is now flat in the size of the menu.
        Map<UUID, List<PublicMenuItem>> itemsByCategory = publicItemsByCategory(website.getId());
        List<Category> allCategories = categoryRepository.findByWebsiteId(website.getId());
        Map<UUID, List<Category>> subcategoriesByParent = allCategories.stream()
                .filter(category -> category.getParent() != null)
                .collect(Collectors.groupingBy(category -> category.getParent().getId()));

        List<PublicWebsiteResponse.PublicCategory> categories = allCategories.stream()
                .filter(category -> category.getParent() == null)
                .map(parent -> {
                    List<PublicWebsiteResponse.PublicCategory> subcategories =
                            subcategoriesByParent.getOrDefault(parent.getId(), List.of()).stream()
                                    .map(sub -> new PublicWebsiteResponse.PublicCategory(
                                            sub.getId().toString(), sub.getName(),
                                            itemsByCategory.getOrDefault(sub.getId(), List.of()), List.of()))
                                    .toList();
                    return new PublicWebsiteResponse.PublicCategory(
                            parent.getId().toString(), parent.getName(),
                            itemsByCategory.getOrDefault(parent.getId(), List.of()), subcategories);
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

        List<PublicWebsiteResponse.PublicProject> projects = portfolioProjectRepository
                .findByWebsiteIdOrderBySortOrder(website.getId()).stream()
                .map(p -> new PublicWebsiteResponse.PublicProject(
                        p.getId().toString(), p.getName(), p.getDiscipline(), p.getYear(), p.getSummary(),
                        PortfolioProjectResponse.splitTags(p.getTags()), p.getImageUrl(), p.getLiveUrl(), p.getRepoUrl()))
                .toList();

        // Null on every non-EVENTS site, and on an EVENTS site nobody has filled
        // in yet - the template renders whatever is there and hides the rest.
        PublicWebsiteResponse.PublicEvent event = eventDetailsRepository.findByWebsiteId(website.getId())
                .map(details -> new PublicWebsiteResponse.PublicEvent(
                        details.getEventDate(), details.getStartTime(), details.getEndTime(),
                        details.getVenueName(), details.getDressCode(), details.getRsvpBy(), details.getNote()))
                .orElse(null);

        List<PublicWebsiteResponse.PublicScheduleEntry> schedule = eventScheduleEntryRepository
                .findByWebsiteIdOrderBySortOrder(website.getId()).stream()
                .map(entry -> new PublicWebsiteResponse.PublicScheduleEntry(
                        entry.getId().toString(), entry.getTime(), entry.getTitle(), entry.getDetail()))
                .toList();

        // Phase 3: every website has an *effective* theme, whether it picked a preset or is building from
        // scratch - re-validated defensively here since the column is still free-text TEXT at the DB level.
        // The website's own overrides win over the preset it started from, and
        // the preset over the built-in defaults. parseOrDefault rather than
        // parseAndValidate at render time: a stored value that no longer parses
        // must not take a published site down.
        ThemeConfig theme;
        if (website.getThemeConfig() != null && !website.getThemeConfig().isBlank()) {
            theme = themeConfigValidator.parseOrDefault(website.getThemeConfig());
        } else if (website.getTheme() != null) {
            theme = themeConfigValidator.parseOrDefault(website.getTheme().getThemeConfig());
        } else {
            theme = ThemeConfig.defaults();
        }

        return new PublicWebsiteResponse(
                website.getBusinessName(), website.getSlug(), website.getPageMode(), website.getTemplateType(),
                website.getEffectiveLayoutVariant(), website.getOrderingMode(), website.getPrimaryLanguage(), website.getCurrency(),
                content, profile, hours, categories, areas, services, galleryUrls, seo, sections, theme, projects,
                event, schedule);
    }

    /**
     * Every public-safe item on the website, grouped by category id, with its
     * sizes, add-on groups, add-ons and box variants already attached.
     *
     * Five queries total regardless of menu size. The previous version took the
     * category id and resolved each item's options one item at a time, so cost
     * grew with the number of items rather than staying flat.
     */
    private Map<UUID, List<PublicMenuItem>> publicItemsByCategory(UUID websiteId) {
        List<MenuItem> items = menuItemRepository.findByWebsiteIdAndTrashedAtIsNull(websiteId);
        if (items.isEmpty()) {
            return Map.of();
        }

        List<UUID> itemIds = items.stream().map(MenuItem::getId).toList();

        Map<UUID, List<SizeVariant>> sizesByItem = sizeVariantRepository.findByMenuItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(size -> size.getMenuItem().getId()));
        Map<UUID, List<BoxVariant>> boxesByItem = boxVariantRepository.findByMenuItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(box -> box.getMenuItem().getId()));

        List<AddonGroup> groups = addonGroupRepository.findByMenuItemIdIn(itemIds);
        Map<UUID, List<AddonGroup>> groupsByItem = groups.stream()
                .collect(Collectors.groupingBy(group -> group.getMenuItem().getId()));
        // Skipped entirely when nothing has add-ons: findByAddonGroupIdIn(empty)
        // would be a query returning nothing.
        Map<UUID, List<Addon>> addonsByGroup = groups.isEmpty()
                ? Map.of()
                : addonRepository.findByAddonGroupIdIn(groups.stream().map(AddonGroup::getId).toList()).stream()
                        .collect(Collectors.groupingBy(addon -> addon.getAddonGroup().getId()));

        return items.stream().collect(Collectors.groupingBy(
                item -> item.getCategory().getId(),
                Collectors.mapping(item -> PublicMenuItem.from(
                        item,
                        sizesByItem.getOrDefault(item.getId(), List.of()),
                        groupsByItem.getOrDefault(item.getId(), List.of()),
                        addonsByGroup,
                        boxesByItem.getOrDefault(item.getId(), List.of())), Collectors.toList())));
    }

}
