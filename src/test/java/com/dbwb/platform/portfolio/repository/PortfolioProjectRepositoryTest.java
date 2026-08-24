package com.dbwb.platform.portfolio.repository;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.AccountStatus;
import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.common.config.JpaAuditingConfig;
import com.dbwb.platform.portfolio.entity.PortfolioProject;
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
 * Regression test for a bug that hid behind a green build: portfolio_projects
 * was never created in the test schema at all.
 *
 * The entity mapped its column as a bare `year`, which H2 reserves as an
 * identifier. Hibernate's ddl-auto CREATE TABLE failed, the failure was logged
 * and swallowed, and the suite went on passing - because not one test touched
 * the table. Renaming the column to project_year is the fix; this test is what
 * stops it silently breaking again, since it can only pass if the table really
 * exists and every column really maps.
 *
 * JpaAuditingConfig is imported explicitly for the same reason as
 * BusinessWebsiteRepositoryTest: @DataJpaTest's slice scanning misses it, and
 * BaseEntity's timestamps are non-null columns only auditing populates.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class PortfolioProjectRepositoryTest {

    @Autowired
    private PortfolioProjectRepository projectRepository;
    @Autowired
    private BusinessWebsiteRepository websiteRepository;
    @Autowired
    private AccountRepository accountRepository;

    @Test
    void persistsAProjectWithEveryColumnMapped() {
        BusinessWebsite website = portfolioWebsite();

        PortfolioProject project = new PortfolioProject();
        project.setWebsite(website);
        project.setName("Rebrand for Aurora");
        project.setDiscipline("Brand identity");
        project.setYear("2023-24");
        project.setSummary("A full visual refresh.");
        project.setTags("branding,print");
        project.setImageUrl("https://example.com/cover.png");
        project.setLiveUrl("https://example.com/aurora");
        project.setRepoUrl("https://example.com/repo");
        project.setSortOrder(0);
        projectRepository.saveAndFlush(project);

        var loaded = projectRepository.findByWebsiteIdOrderBySortOrder(website.getId());

        assertThat(loaded).singleElement().satisfies(p -> {
            assertThat(p.getName()).isEqualTo("Rebrand for Aurora");
            // The column this test exists for: written and read back under its
            // new name, with the Java field still called year.
            assertThat(p.getYear()).isEqualTo("2023-24");
            assertThat(p.getDiscipline()).isEqualTo("Brand identity");
            assertThat(p.getTags()).isEqualTo("branding,print");
        });
    }

    @Test
    void listsAWebsitesProjectsInSortOrder() {
        BusinessWebsite website = portfolioWebsite();
        projectRepository.save(project(website, "Third", 2));
        projectRepository.save(project(website, "First", 0));
        projectRepository.save(project(website, "Second", 1));
        projectRepository.flush();

        assertThat(projectRepository.findByWebsiteIdOrderBySortOrder(website.getId()))
                .extracting(PortfolioProject::getName)
                .containsExactly("First", "Second", "Third");
    }

    @Test
    void keepsOneWebsitesProjectsOutOfAnothersList() {
        BusinessWebsite mine = portfolioWebsite();
        BusinessWebsite theirs = portfolioWebsite();
        projectRepository.save(project(mine, "Mine", 0));
        projectRepository.save(project(theirs, "Theirs", 0));
        projectRepository.flush();

        assertThat(projectRepository.findByWebsiteIdOrderBySortOrder(mine.getId()))
                .extracting(PortfolioProject::getName)
                .containsExactly("Mine");
    }

    private PortfolioProject project(BusinessWebsite website, String name, int sortOrder) {
        PortfolioProject project = new PortfolioProject();
        project.setWebsite(website);
        project.setName(name);
        project.setSortOrder(sortOrder);
        return project;
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
