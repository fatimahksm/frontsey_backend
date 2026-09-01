package com.dbwb.platform.publicapi;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.AccountStatus;
import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.menu.entity.Addon;
import com.dbwb.platform.menu.entity.AddonGroup;
import com.dbwb.platform.menu.entity.BoxVariant;
import com.dbwb.platform.menu.entity.Category;
import com.dbwb.platform.menu.entity.ItemAvailability;
import com.dbwb.platform.menu.entity.MenuItem;
import com.dbwb.platform.menu.entity.SizeVariant;
import com.dbwb.platform.menu.repository.AddonGroupRepository;
import com.dbwb.platform.menu.repository.AddonRepository;
import com.dbwb.platform.menu.repository.BoxVariantRepository;
import com.dbwb.platform.menu.repository.CategoryRepository;
import com.dbwb.platform.menu.repository.MenuItemRepository;
import com.dbwb.platform.menu.repository.SizeVariantRepository;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.PageMode;
import com.dbwb.platform.website.entity.WebsiteStatus;
import com.dbwb.platform.common.config.CacheConfig;
import com.dbwb.platform.website.repository.BusinessWebsiteRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins down the fix for an N+1 that sat on the hottest path in the product.
 *
 * PublicWebsiteService used to resolve a menu one category and one item at a
 * time: a query per parent category to find its children, then four more per
 * item for sizes, add-on groups, add-ons and box variants. A 100-item menu ran
 * over 400 queries on every single public page load, uncached, against the
 * 3-second target in BR-NFR-001.
 *
 * A count assertion alone would rot - somebody tunes the payload, the number
 * moves, the number gets updated, and the N+1 creeps back unnoticed. So this
 * asserts the shape instead: assembling a menu five times the size must not
 * cost more queries. That statement stays true only while the loading is
 * genuinely batched.
 */
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class PublicWebsiteQueryCountTest {

    @Autowired private PublicWebsiteService publicWebsiteService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private BusinessWebsiteRepository websiteRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private SizeVariantRepository sizeVariantRepository;
    @Autowired private AddonGroupRepository addonGroupRepository;
    @Autowired private AddonRepository addonRepository;
    @Autowired private BoxVariantRepository boxVariantRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private org.springframework.cache.CacheManager cacheManager;

    @Test
    @Transactional
    void assemblingABiggerMenuDoesNotCostMoreQueries() {
        String smallSlug = seedMenu(2);
        String largeSlug = seedMenu(10);

        // Warm up first. The very first assembly in a context also prepares
        // statements Hibernate then reuses, so measuring it makes the result
        // depend on whether this class ran first in the suite - which is how
        // this test managed to fail in a full run and pass on its own.
        publicWebsiteService.getBySlug(smallSlug);
        publicWebsiteService.getBySlug(largeSlug);

        // Measured: 15 and 15 after the fix. Before it, 20 and 52 - four more
        // queries for every item added.
        long smallMenuQueries = queriesToAssemble(smallSlug);
        long largeMenuQueries = queriesToAssemble(largeSlug);

        assertThat(largeMenuQueries)
                .as("a five-times-bigger menu must not cost more queries - that is what makes it batched")
                .isEqualTo(smallMenuQueries);
    }

    @Test
    @Transactional
    void stillReturnsEveryItemWithItsOptionsAttached() {
        String slug = seedMenu(3);

        var response = publicWebsiteService.getBySlug(slug).website();

        assertThat(response.categories()).singleElement().satisfies(category -> {
            assertThat(category.items()).hasSize(3);
            assertThat(category.items()).allSatisfy(item -> {
                assertThat(item.sizes()).extracting("label").containsExactly("Regular");
                assertThat(item.boxVariants()).extracting("label").containsExactly("Box of 6");
                assertThat(item.addonGroups()).singleElement().satisfies(group ->
                        assertThat(group.options()).extracting("name").containsExactly("Extra shot"));
            });
        });
    }

    /**
     * Counts only the queries the assembly itself issues, with the seed data
     * already flushed.
     *
     * The cache is cleared first on purpose. This test exists to measure what
     * building a page costs, and a warm cache would report zero queries and
     * pass for entirely the wrong reason - the N+1 could come back and this
     * would still be green.
     */
    private long queriesToAssemble(String slug) {
        cacheManager.getCache(CacheConfig.PUBLIC_WEBSITES).clear();
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        publicWebsiteService.getBySlug(slug);

        return statistics.getPrepareStatementCount();
    }

    /** One website, one category, and {@code itemCount} items each carrying a size, an add-on group with one add-on, and a box variant. */
    private String seedMenu(int itemCount) {
        Account owner = new Account();
        owner.setEmail("owner-" + System.nanoTime() + "@example.com");
        owner.setPasswordHash("hash");
        owner.setFullName("Owner");
        owner.setRole(Role.BUSINESS_OWNER);
        owner.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(owner);

        BusinessWebsite website = new BusinessWebsite();
        website.setOwner(owner);
        website.setBusinessName("Query Count Cafe");
        website.setSlug("qc-" + System.nanoTime());
        website.setPageMode(PageMode.MULTI_PAGE);
        website.setStatus(WebsiteStatus.PUBLISHED);
        websiteRepository.save(website);

        Category category = new Category();
        category.setWebsite(website);
        category.setName("Coffee");
        categoryRepository.save(category);

        for (int i = 0; i < itemCount; i++) {
            MenuItem item = new MenuItem();
            item.setWebsite(website);
            item.setCategory(category);
            item.setName("Item " + i);
            item.setPrice(new BigDecimal("4.50"));
            item.setAvailability(ItemAvailability.AVAILABLE);
            menuItemRepository.save(item);

            SizeVariant size = new SizeVariant();
            size.setMenuItem(item);
            size.setLabel("Regular");
            size.setPrice(new BigDecimal("4.50"));
            sizeVariantRepository.save(size);

            AddonGroup group = new AddonGroup();
            group.setMenuItem(item);
            group.setName("Extras");
            addonGroupRepository.save(group);

            Addon addon = new Addon();
            addon.setAddonGroup(group);
            addon.setName("Extra shot");
            addon.setExtraPrice(new BigDecimal("0.75"));
            addonRepository.save(addon);

            BoxVariant box = new BoxVariant();
            box.setMenuItem(item);
            box.setLabel("Box of 6");
            box.setUnitCount(6);
            box.setPrice(new BigDecimal("24.00"));
            boxVariantRepository.save(box);
        }

        return website.getSlug();
    }
}
