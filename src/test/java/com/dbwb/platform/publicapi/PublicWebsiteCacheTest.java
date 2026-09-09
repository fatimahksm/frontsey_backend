package com.dbwb.platform.publicapi;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.AccountStatus;
import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.common.config.CacheConfig;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.PageMode;
import com.dbwb.platform.website.entity.WebsiteStatus;
import com.dbwb.platform.website.repository.BusinessWebsiteRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The public page is assembled from about a dozen tables into 38KB of JSON on
 * every visit. Measured under load, that - not the database, which answered in
 * about 0.2ms - was what capped the server at roughly 480 requests a second.
 */
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class PublicWebsiteCacheTest {

    @Autowired private PublicWebsiteService publicWebsiteService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private BusinessWebsiteRepository websiteRepository;
    @Autowired private CacheManager cacheManager;
    @Autowired private EntityManager entityManager;

    private String slug;

    @BeforeEach
    void setUp() {
        cacheManager.getCache(CacheConfig.PUBLIC_WEBSITES).clear();
        slug = seedPublishedSite();
    }

    @Test
    @Transactional
    void asecondVisitCostsNoQueriesAtAll() {
        long first = queriesFor(slug);
        long second = queriesFor(slug);

        assertThat(first).as("the first visit still does the work").isGreaterThan(0);
        assertThat(second).as("the second is served from the cache").isZero();
    }

    @Test
    @Transactional
    void differentWebsitesAreCachedSeparately() {
        // Keyed by slug: one busy restaurant's page must not be served to
        // another's visitors, which is the failure that would matter here.
        String other = seedPublishedSite();

        var one = publicWebsiteService.lookupBySlug(slug);
        var two = publicWebsiteService.lookupBySlug(other);

        assertThat(one.websiteId()).isNotEqualTo(two.websiteId());
        assertThat(publicWebsiteService.lookupBySlug(slug).websiteId()).isEqualTo(one.websiteId());
        assertThat(publicWebsiteService.lookupBySlug(other).websiteId()).isEqualTo(two.websiteId());
    }

    @Test
    @Transactional
    void aCachedPageStillCarriesTheWebsiteIdSoTheVisitIsStillCounted() {
        // Recording the visit lives in the controller, not behind the cache. A
        // cached page is still a page view, and analytics that only counted
        // misses would undercount by whatever the hit rate happened to be.
        var cold = publicWebsiteService.lookupBySlug(slug);
        var warm = publicWebsiteService.lookupBySlug(slug);

        assertThat(warm.websiteId()).isNotNull().isEqualTo(cold.websiteId());
    }

    @Test
    @Transactional
    void anUnknownSlugIsCachedToo() {
        // Otherwise a flood of requests for slugs that do not exist is the one
        // traffic pattern the cache does nothing for.
        assertThat(queriesFor("no-such-site")).isGreaterThan(0);
        assertThat(queriesFor("no-such-site")).isZero();
    }

    private long queriesFor(String forSlug) {
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        publicWebsiteService.lookupBySlug(forSlug);

        return statistics.getPrepareStatementCount();
    }

    private String seedPublishedSite() {
        Account owner = new Account();
        owner.setEmail("cache-" + System.nanoTime() + "@example.com");
        owner.setPasswordHash("hash");
        owner.setFullName("Owner");
        owner.setRole(Role.BUSINESS_OWNER);
        owner.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(owner);

        BusinessWebsite website = new BusinessWebsite();
        website.setOwner(owner);
        website.setBusinessName("Cached Cafe");
        website.setSlug("cached-" + System.nanoTime());
        website.setPageMode(PageMode.MULTI_PAGE);
        website.setStatus(WebsiteStatus.PUBLISHED);
        return websiteRepository.save(website).getSlug();
    }
}
