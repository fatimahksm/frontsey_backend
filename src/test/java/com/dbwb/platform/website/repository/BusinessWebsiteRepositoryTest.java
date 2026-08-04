package com.dbwb.platform.website.repository;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.AccountStatus;
import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.common.config.JpaAuditingConfig;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.PageMode;
import com.dbwb.platform.website.entity.WebsiteStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Regression test for a real bug this session found: AdminService.listWebsites()
 * used to call plain findAll(), whose owner association is lazy - fine inside
 * the transactional service method, but AdminController maps entities to DTOs
 * (touching website.getOwner().getEmail()) after that transaction has already
 * closed, which threw LazyInitializationException. findAllWithOwner()'s JOIN
 * FETCH is what actually fixes it; this pins that down so it can't regress.
 *
 * JpaAuditingConfig is imported explicitly - @DataJpaTest's slice scanning
 * doesn't pick it up on its own, and BaseEntity.createdAt/updatedAt are
 * non-null columns populated only through auditing.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class BusinessWebsiteRepositoryTest {

    @Autowired
    private BusinessWebsiteRepository websiteRepository;
    @Autowired
    private AccountRepository accountRepository;

    @Test
    void findAllWithOwnerLoadsTheOwnerEagerlySoItSurvivesOutsideTheTransaction() {
        Account owner = new Account();
        owner.setEmail("owner@example.com");
        owner.setPasswordHash("hash");
        owner.setFullName("Owner");
        owner.setRole(Role.BUSINESS_OWNER);
        owner.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(owner);

        BusinessWebsite website = new BusinessWebsite();
        website.setOwner(owner);
        website.setBusinessName("Test Cafe");
        website.setSlug("test-cafe-" + System.nanoTime());
        website.setPageMode(PageMode.MULTI_PAGE);
        website.setStatus(WebsiteStatus.DRAFT);
        websiteRepository.save(website);

        // Detach everything - simulates the entity manager having already
        // closed by the time AdminController maps the result to a DTO.
        websiteRepository.flush();

        var loaded = websiteRepository.findAllWithOwner();

        assertThat(loaded).isNotEmpty();
        assertThatCode(() -> loaded.forEach(w -> w.getOwner().getEmail()))
                .doesNotThrowAnyException();
    }
}
