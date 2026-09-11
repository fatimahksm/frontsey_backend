package com.dbwb.platform.portfolio.repository;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.AccountStatus;
import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.common.config.JpaAuditingConfig;
import com.dbwb.platform.portfolio.entity.ExperienceEntry;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.PageMode;
import com.dbwb.platform.website.entity.TemplateType;
import com.dbwb.platform.website.entity.WebsiteStatus;
import com.dbwb.platform.website.repository.BusinessWebsiteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persists a row for real, which is the only thing that proves the table maps.
 *
 * portfolio_projects taught this the hard way: its column was called `year`,
 * H2 reserves that as an identifier, Hibernate's CREATE TABLE failed, the
 * failure was swallowed, and the whole suite stayed green because no test ever
 * touched the table. This entity has a year column too - named entry_year for
 * exactly that reason - so it gets the test that would have caught it.
 *
 * JpaAuditingConfig is imported explicitly because @DataJpaTest's slice
 * scanning misses it, and BaseEntity's timestamps are non-null columns that
 * only auditing fills in.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class ExperienceEntryRepositoryTest {

    @Autowired
    private ExperienceEntryRepository experienceRepository;
    @Autowired
    private BusinessWebsiteRepository websiteRepository;
    @Autowired
    private AccountRepository accountRepository;

    @Test
    void persistsAnEntryWithEveryColumnMapped() {
        BusinessWebsite website = portfolioWebsite();

        ExperienceEntry entry = new ExperienceEntry();
        entry.setWebsite(website);
        entry.setRole("Lead designer");
        entry.setCompany("Studio Beirut");
        entry.setYear("2021 - Present");
        entry.setDetail("Brand and product work for regional clients.");
        entry.setSortOrder(0);
        experienceRepository.saveAndFlush(entry);

        assertThat(experienceRepository.findByWebsiteIdOrderBySortOrder(website.getId()))
                .singleElement()
                .satisfies(saved -> {
                    assertThat(saved.getRole()).isEqualTo("Lead designer");
                    assertThat(saved.getCompany()).isEqualTo("Studio Beirut");
                    // The column this test exists for: free text, written and
                    // read back under a name H2 does not reserve.
                    assertThat(saved.getYear()).isEqualTo("2021 - Present");
                    assertThat(saved.getDetail()).isEqualTo("Brand and product work for regional clients.");
                });
    }

    @Test
    void listsAWebsitesEntriesInSortOrder() {
        BusinessWebsite website = portfolioWebsite();
        experienceRepository.save(entry(website, "Third", 2));
        experienceRepository.save(entry(website, "First", 0));
        experienceRepository.save(entry(website, "Second", 1));
        experienceRepository.flush();

        assertThat(experienceRepository.findByWebsiteIdOrderBySortOrder(website.getId()))
                .extracting(ExperienceEntry::getRole)
                .containsExactly("First", "Second", "Third");
    }

    @Test
    void keepsOneWebsitesEntriesOutOfAnothersList() {
        BusinessWebsite mine = portfolioWebsite();
        BusinessWebsite theirs = portfolioWebsite();
        experienceRepository.save(entry(mine, "Mine", 0));
        experienceRepository.save(entry(theirs, "Theirs", 0));
        experienceRepository.flush();

        assertThat(experienceRepository.findByWebsiteIdOrderBySortOrder(mine.getId()))
                .extracting(ExperienceEntry::getRole)
                .containsExactly("Mine");
    }

    /** Only the role is required, and a bare entry has to survive a round trip. */
    @Test
    void persistsAnEntryWithNothingButARole() {
        BusinessWebsite website = portfolioWebsite();
        experienceRepository.saveAndFlush(entry(website, "Freelance", 0));

        assertThat(experienceRepository.findByWebsiteIdOrderBySortOrder(website.getId()))
                .singleElement()
                .satisfies(saved -> {
                    assertThat(saved.getRole()).isEqualTo("Freelance");
                    assertThat(saved.getCompany()).isNull();
                    assertThat(saved.getYear()).isNull();
                });
    }

    private ExperienceEntry entry(BusinessWebsite website, String role, int sortOrder) {
        ExperienceEntry entry = new ExperienceEntry();
        entry.setWebsite(website);
        entry.setRole(role);
        entry.setSortOrder(sortOrder);
        return entry;
    }

    private BusinessWebsite portfolioWebsite() {
        Account owner = new Account();
        owner.setEmail("owner-" + System.nanoTime() + "@example.com");
        owner.setPasswordHash("hash");
        owner.setFullName("Owner");
        owner.setRole(Role.BUSINESS_OWNER);
        owner.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(owner);

        BusinessWebsite website = new BusinessWebsite();
        website.setOwner(owner);
        website.setBusinessName("Aurora Studio");
        website.setSlug("aurora-" + System.nanoTime());
        website.setPageMode(PageMode.MULTI_PAGE);
        website.setTemplateType(TemplateType.PORTFOLIO);
        website.setStatus(WebsiteStatus.DRAFT);
        return websiteRepository.save(website);
    }
}
