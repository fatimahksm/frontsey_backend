package com.dbwb.platform.publicapi;

import com.dbwb.platform.delivery.repository.DeliveryAreaRepository;
import com.dbwb.platform.gallery.repository.GalleryImageRepository;
import com.dbwb.platform.menu.repository.AddonGroupRepository;
import com.dbwb.platform.menu.repository.AddonRepository;
import com.dbwb.platform.menu.repository.BoxVariantRepository;
import com.dbwb.platform.menu.repository.CategoryRepository;
import com.dbwb.platform.menu.repository.MenuItemRepository;
import com.dbwb.platform.menu.repository.SizeVariantRepository;
import com.dbwb.platform.portfolio.repository.ServiceItemRepository;
import com.dbwb.platform.profile.repository.BusinessProfileRepository;
import com.dbwb.platform.profile.repository.OpeningHoursRepository;
import com.dbwb.platform.sections.repository.PageSectionRepository;
import com.dbwb.platform.testsupport.TestEntities;
import com.dbwb.platform.theme.ThemeConfigValidator;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.PageMode;
import com.dbwb.platform.website.entity.WebsiteStatus;
import com.dbwb.platform.website.repository.BusinessWebsiteRepository;
import com.dbwb.platform.website.repository.SeoMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicWebsiteServiceTest {

    @Mock private BusinessWebsiteRepository websiteRepository;
    @Mock private BusinessProfileRepository profileRepository;
    @Mock private OpeningHoursRepository openingHoursRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private MenuItemRepository menuItemRepository;
    @Mock private SizeVariantRepository sizeVariantRepository;
    @Mock private AddonGroupRepository addonGroupRepository;
    @Mock private AddonRepository addonRepository;
    @Mock private BoxVariantRepository boxVariantRepository;
    @Mock private DeliveryAreaRepository deliveryAreaRepository;
    @Mock private GalleryImageRepository galleryImageRepository;
    @Mock private SeoMetadataRepository seoMetadataRepository;
    @Mock private ServiceItemRepository serviceItemRepository;
    @Mock private PageSectionRepository pageSectionRepository;
    @Mock private ThemeConfigValidator themeConfigValidator;

    private PublicWebsiteService service;

    @BeforeEach
    void setUp() {
        service = new PublicWebsiteService(
                websiteRepository, profileRepository, openingHoursRepository, categoryRepository, menuItemRepository,
                sizeVariantRepository, addonGroupRepository, addonRepository, boxVariantRepository,
                deliveryAreaRepository, galleryImageRepository, seoMetadataRepository, serviceItemRepository,
                pageSectionRepository, themeConfigValidator);
    }

    private BusinessWebsite websiteWith(String draftContent, String publishedContent) {
        BusinessWebsite website = TestEntities.withId(new BusinessWebsite(), UUID.randomUUID());
        website.setBusinessName("Test Cafe");
        website.setSlug("test-cafe");
        website.setPageMode(PageMode.ONE_PAGE);
        website.setStatus(WebsiteStatus.PUBLISHED);
        website.setDraftContent(draftContent);
        website.setPublishedContent(publishedContent);
        return website;
    }

    @Test
    void publicSlugLookupUsesThePublishedContentNotTheDraft() {
        BusinessWebsite website = websiteWith("{\"heroHeading\":\"unpublished draft copy\"}", "{\"heroHeading\":\"live copy\"}");
        when(websiteRepository.findBySlug("test-cafe")).thenReturn(java.util.Optional.of(website));

        var envelope = service.getBySlug("test-cafe");

        assertThat(envelope.website().publishedContent()).isEqualTo("{\"heroHeading\":\"live copy\"}");
    }

    @Test
    void ownerPreviewUsesTheDraftContentEvenWhenDifferentFromPublished() {
        BusinessWebsite website = websiteWith("{\"heroHeading\":\"unpublished draft copy\"}", "{\"heroHeading\":\"live copy\"}");

        var preview = service.assembleForPreview(website);

        assertThat(preview.publishedContent()).isEqualTo("{\"heroHeading\":\"unpublished draft copy\"}");
    }

    @Test
    void ownerPreviewWorksEvenForAnUnpublishedDraftWebsite() {
        BusinessWebsite website = websiteWith("{\"heroHeading\":\"still drafting\"}", null);
        website.setStatus(WebsiteStatus.DRAFT);

        var preview = service.assembleForPreview(website);

        assertThat(preview.publishedContent()).isEqualTo("{\"heroHeading\":\"still drafting\"}");
    }
}
