package com.dbwb.platform.subscription.repository;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.AccountStatus;
import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.common.config.JpaAuditingConfig;
import com.dbwb.platform.plan.entity.BillingPeriod;
import com.dbwb.platform.plan.entity.Plan;
import com.dbwb.platform.plan.entity.PlanCode;
import com.dbwb.platform.plan.repository.PlanRepository;
import com.dbwb.platform.subscription.entity.Subscription;
import com.dbwb.platform.subscription.entity.SubscriptionStatus;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.PageMode;
import com.dbwb.platform.website.entity.WebsiteStatus;
import com.dbwb.platform.website.repository.BusinessWebsiteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Regression test for the same class of bug BusinessWebsiteRepositoryTest
 * pins down: SubscriptionController.get() calls SubscriptionResponse.from(),
 * which reads subscription.getPlan().getCode() in the controller - after
 * SubscriptionService's read-only transaction has already closed. Plan is a
 * lazy @ManyToOne, so plain findByWebsiteId() left a proxy that threw
 * LazyInitializationException; findByWebsiteIdWithPlan()'s JOIN FETCH fixes it.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class SubscriptionRepositoryTest {

    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private BusinessWebsiteRepository websiteRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PlanRepository planRepository;

    @Test
    void findByWebsiteIdWithPlanLoadsThePlanEagerlySoItSurvivesOutsideTheTransaction() {
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

        Plan plan = new Plan();
        plan.setCode(PlanCode.BASIC);
        plan.setBillingPeriod(BillingPeriod.MONTHLY);
        plan.setPrice(new BigDecimal("9.99"));
        planRepository.save(plan);

        Subscription subscription = new Subscription();
        subscription.setWebsite(website);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);

        // Detach everything - simulates the entity manager having already
        // closed by the time SubscriptionController maps the result to a DTO.
        subscriptionRepository.flush();

        var loaded = subscriptionRepository.findByWebsiteIdWithPlan(website.getId());

        assertThat(loaded).isPresent();
        assertThatCode(() -> loaded.get().getPlan().getCode()).doesNotThrowAnyException();
        assertThat(loaded.get().getPlan().getCode()).isEqualTo(PlanCode.BASIC);
    }
}
