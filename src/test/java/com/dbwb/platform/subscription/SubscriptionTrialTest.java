package com.dbwb.platform.subscription;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.audit.AuditService;
import com.dbwb.platform.common.config.BusinessRuleProperties;
import com.dbwb.platform.notification.EmailService;
import com.dbwb.platform.plan.entity.BillingPeriod;
import com.dbwb.platform.plan.entity.Plan;
import com.dbwb.platform.plan.entity.PlanCode;
import com.dbwb.platform.plan.repository.PlanRepository;
import com.dbwb.platform.plan.repository.TemplatePriceRepository;
import com.dbwb.platform.subscription.entity.MockPayment;
import com.dbwb.platform.subscription.entity.MockPaymentStatus;
import com.dbwb.platform.subscription.entity.Subscription;
import com.dbwb.platform.subscription.entity.SubscriptionStatus;
import com.dbwb.platform.subscription.repository.MockPaymentRepository;
import com.dbwb.platform.subscription.repository.SubscriptionRepository;
import com.dbwb.platform.testsupport.TestEntities;
import com.dbwb.platform.website.WebsiteAccessGuard;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.WebsiteStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * The free trial: a website may be created, filled in and published without
 * anyone being asked to pay, and it stops on its own a week later.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionTrialTest {

    private static final int TRIAL_DAYS = 7;

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private MockPaymentRepository mockPaymentRepository;
    @Mock private PlanRepository planRepository;
    @Mock private TemplatePriceRepository templatePriceRepository;
    @Mock private WebsiteAccessGuard accessGuard;
    @Mock private EmailService emailService;
    @Mock private AuditService auditService;

    private SubscriptionService service;
    private SubscriptionQueryService queryService;
    private BusinessWebsite website;
    private Plan basicMonthly;

    @BeforeEach
    void setUp() {
        BusinessRuleProperties rules = new BusinessRuleProperties();
        rules.setSubscriptionTrialDays(TRIAL_DAYS);
        rules.setSubscriptionGracePeriodDays(3);

        service = new SubscriptionService(subscriptionRepository, mockPaymentRepository, planRepository, templatePriceRepository,
                accessGuard, rules, emailService, auditService);
        queryService = new SubscriptionQueryService(subscriptionRepository);

        Account owner = TestEntities.withId(new Account(), UUID.randomUUID());
        owner.setEmail("owner@example.com");

        website = TestEntities.withId(new BusinessWebsite(), UUID.randomUUID());
        website.setOwner(owner);

        basicMonthly = new Plan();
        basicMonthly.setCode(PlanCode.BASIC);
        basicMonthly.setBillingPeriod(BillingPeriod.MONTHLY);
        basicMonthly.setPrice(new BigDecimal("9.99"));

        lenient().when(planRepository.findByCodeAndBillingPeriod(PlanCode.BASIC, BillingPeriod.MONTHLY))
                .thenReturn(Optional.of(basicMonthly));
        lenient().when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void firstPublishOpensATrialThatRunsForTheConfiguredWindow() {
        when(subscriptionRepository.findByWebsiteId(website.getId())).thenReturn(Optional.empty());

        Subscription trial = service.startTrialIfEligible(website).orElseThrow();

        assertThat(trial.getStatus()).isEqualTo(SubscriptionStatus.TRIAL);
        assertThat(trial.getPlan()).isEqualTo(basicMonthly);
        assertThat(trial.getEndDate()).isCloseTo(
                Instant.now().plus(TRIAL_DAYS, ChronoUnit.DAYS), within(1, ChronoUnit.MINUTES));
    }

    @Test
    void aMisconfiguredTrialLengthNeverProducesAZeroDayTrial() {
        // An unset dbwb.business-rules.subscription-trial-days binds to 0, which
        // made startTrialIfEligible write endDate == startDate: a "trial" that
        // was already over the moment it began, so the very next maintenance
        // pass expired it and took the freshly published site offline. The
        // owner saw "Started Aug 17 - Ends Aug 17 - Expired" and no trial at all.
        BusinessRuleProperties unset = new BusinessRuleProperties();
        SubscriptionService misconfigured = new SubscriptionService(subscriptionRepository, mockPaymentRepository,
                planRepository, templatePriceRepository, accessGuard, unset, emailService, auditService);
        when(subscriptionRepository.findByWebsiteId(website.getId())).thenReturn(Optional.empty());

        Subscription trial = misconfigured.startTrialIfEligible(website).orElseThrow();

        assertThat(trial.getEndDate()).isAfter(trial.getStartDate());
        assertThat(trial.getEndDate()).isCloseTo(
                Instant.now().plus(SubscriptionService.DEFAULT_TRIAL_DAYS, ChronoUnit.DAYS), within(1, ChronoUnit.MINUTES));
    }

    @Test
    void aTrialThatNeverActuallyRanIsNotCountedAsUsed() {
        // Repairs the websites the zero-day bug already burned: their one trial
        // was spent without a single day of it being served.
        Subscription burned = subscriptionWith(SubscriptionStatus.EXPIRED);
        Instant sameInstant = Instant.now().minus(2, ChronoUnit.HOURS);
        burned.setStartDate(sameInstant);
        burned.setEndDate(sameInstant);
        burned.setGraceEndsAt(null);
        when(subscriptionRepository.findByWebsiteId(website.getId())).thenReturn(Optional.of(burned));

        Subscription reopened = service.startTrialIfEligible(website).orElseThrow();

        assertThat(reopened.getStatus()).isEqualTo(SubscriptionStatus.TRIAL);
        assertThat(reopened.getEndDate()).isCloseTo(
                Instant.now().plus(TRIAL_DAYS, ChronoUnit.DAYS), within(1, ChronoUnit.MINUTES));
    }

    @Test
    void aTrialThatRanItsCourseStaysUsed() {
        Subscription spent = subscriptionWith(SubscriptionStatus.EXPIRED);
        spent.setStartDate(Instant.now().minus(20, ChronoUnit.DAYS));
        spent.setEndDate(Instant.now().minus(10, ChronoUnit.DAYS));
        spent.setGraceEndsAt(null);
        when(subscriptionRepository.findByWebsiteId(website.getId())).thenReturn(Optional.of(spent));

        assertThat(service.startTrialIfEligible(website)).isEmpty();
    }

    @Test
    void aTrialHasNoGracePeriod() {
        when(subscriptionRepository.findByWebsiteId(website.getId())).thenReturn(Optional.empty());

        assertThat(service.startTrialIfEligible(website).orElseThrow().getGraceEndsAt()).isNull();
    }

    @Test
    void aWebsiteGetsAtMostOneTrial() {
        when(subscriptionRepository.findByWebsiteId(website.getId()))
                .thenReturn(Optional.of(subscriptionWith(SubscriptionStatus.EXPIRED)));

        assertThat(service.startTrialIfEligible(website)).isEmpty();
    }

    @Test
    void aWebsiteOnItsTrialIsPubliclyVisible() {
        when(subscriptionRepository.findByWebsiteId(website.getId()))
                .thenReturn(Optional.of(subscriptionWith(SubscriptionStatus.TRIAL)));

        assertThat(queryService.hasPublishableSubscription(website.getId())).isTrue();
    }

    @Test
    void aFinishedTrialStopsTheSiteWithoutAGracePeriod() {
        Subscription expiredTrial = subscriptionWith(SubscriptionStatus.TRIAL);
        expiredTrial.setEndDate(Instant.now().minus(1, ChronoUnit.HOURS));
        website.setStatus(WebsiteStatus.PUBLISHED);

        when(subscriptionRepository.findByStatusAndEndDateBefore(eqStatus(SubscriptionStatus.TRIAL), any()))
                .thenReturn(List.of(expiredTrial));
        when(subscriptionRepository.findByStatusAndEndDateBefore(eqStatus(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(List.of());
        when(subscriptionRepository.findByStatusAndGraceEndsAtBefore(any(), any())).thenReturn(List.of());

        service.runLifecycleMaintenance();

        assertThat(expiredTrial.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThat(website.getStatus()).isEqualTo(WebsiteStatus.EXPIRED);
    }

    @Test
    void payingDuringATrialStartsThePaidMonthNowRatherThanStackingOnTheFreeDays() {
        Subscription trial = subscriptionWith(SubscriptionStatus.TRIAL);
        Instant trialEnd = Instant.now().plus(5, ChronoUnit.DAYS);
        trial.setEndDate(trialEnd);

        MockPayment payment = new MockPayment();
        payment.setSubscription(trial);
        payment.setAmount(basicMonthly.getPrice());
        UUID paymentId = UUID.randomUUID();
        TestEntities.withId(payment, paymentId);
        when(mockPaymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        service.resolvePaymentOutcome(paymentId, MockPaymentStatus.SUCCESS);

        assertThat(trial.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        // A month from today, not a month from the end of the free days.
        assertThat(trial.getEndDate()).isCloseTo(
                Instant.now().plus(30, ChronoUnit.DAYS), within(2, ChronoUnit.DAYS));
        assertThat(trial.getEndDate()).isAfter(trialEnd.plus(20, ChronoUnit.DAYS));
    }

    private Subscription subscriptionWith(SubscriptionStatus status) {
        Subscription subscription = TestEntities.withId(new Subscription(), UUID.randomUUID());
        subscription.setWebsite(website);
        subscription.setPlan(basicMonthly);
        subscription.setStatus(status);
        return subscription;
    }

    private static SubscriptionStatus eqStatus(SubscriptionStatus status) {
        return org.mockito.ArgumentMatchers.eq(status);
    }
}
