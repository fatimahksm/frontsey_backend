package com.dbwb.platform.subscription;

import com.dbwb.platform.audit.AuditService;
import com.dbwb.platform.common.config.BusinessRuleProperties;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.notification.EmailService;
import com.dbwb.platform.plan.entity.BillingPeriod;
import com.dbwb.platform.plan.entity.Plan;
import com.dbwb.platform.plan.entity.PlanCode;
import com.dbwb.platform.plan.entity.TemplatePrice;
import com.dbwb.platform.plan.repository.PlanRepository;
import com.dbwb.platform.plan.repository.TemplatePriceRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.subscription.dto.CheckoutRequest;
import com.dbwb.platform.subscription.entity.MockPayment;
import com.dbwb.platform.subscription.entity.Subscription;
import com.dbwb.platform.subscription.entity.SubscriptionStatus;
import com.dbwb.platform.subscription.repository.MockPaymentRepository;
import com.dbwb.platform.subscription.repository.SubscriptionRepository;
import com.dbwb.platform.testsupport.TestEntities;
import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.website.WebsiteAccessGuard;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.LayoutVariant;
import com.dbwb.platform.website.entity.TemplateType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * What a website costs comes from its template, not from a tier the owner
 * picks - they choose a template, then monthly or yearly.
 */
@ExtendWith(MockitoExtension.class)
class TemplatePricingTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private MockPaymentRepository mockPaymentRepository;
    @Mock private PlanRepository planRepository;
    @Mock private TemplatePriceRepository templatePriceRepository;
    @Mock private WebsiteAccessGuard accessGuard;
    @Mock private EmailService emailService;
    @Mock private AuditService auditService;

    private SubscriptionService service;
    private BusinessWebsite website;
    private final UUID websiteId = UUID.randomUUID();
    private final AuthenticatedAccount owner =
            new AuthenticatedAccount(UUID.randomUUID(), "owner@example.com", Role.BUSINESS_OWNER);

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(subscriptionRepository, mockPaymentRepository, planRepository,
                templatePriceRepository, accessGuard, new BusinessRuleProperties(), emailService, auditService);

        Account account = TestEntities.withId(new Account(), owner.accountId());
        account.setEmail(owner.email());
        website = TestEntities.withId(new BusinessWebsite(), websiteId);
        website.setOwner(account);
        website.setTemplateType(TemplateType.MENU_ORDERING);
        website.setLayoutVariant(LayoutVariant.MENU_GRID);

        lenient().when(accessGuard.requireOwner(websiteId, owner)).thenReturn(website);
        lenient().when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(mockPaymentRepository.save(any(MockPayment.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(subscriptionRepository.findByWebsiteId(websiteId)).thenReturn(Optional.empty());
    }

    private void priceThisTemplate(String monthly, String yearly, PlanCode planCode) {
        TemplatePrice price = new TemplatePrice();
        price.setLayoutVariant(LayoutVariant.MENU_GRID);
        price.setMonthlyPrice(new BigDecimal(monthly));
        price.setYearlyPrice(new BigDecimal(yearly));
        price.setPlanCode(planCode);
        price.setActive(true);
        lenient().when(templatePriceRepository.findByLayoutVariant(LayoutVariant.MENU_GRID))
                .thenReturn(Optional.of(price));

        Plan plan = new Plan();
        plan.setCode(planCode);
        plan.setBillingPeriod(BillingPeriod.MONTHLY);
        // Deliberately different from the template price, so a test that reads the
        // wrong one fails loudly rather than coincidentally passing.
        plan.setPrice(new BigDecimal("9.99"));
        lenient().when(planRepository.findByCodeAndBillingPeriod(planCode, BillingPeriod.MONTHLY))
                .thenReturn(Optional.of(plan));

        Plan yearlyPlan = new Plan();
        yearlyPlan.setCode(planCode);
        yearlyPlan.setBillingPeriod(BillingPeriod.YEARLY);
        yearlyPlan.setPrice(new BigDecimal("99.99"));
        lenient().when(planRepository.findByCodeAndBillingPeriod(planCode, BillingPeriod.YEARLY))
                .thenReturn(Optional.of(yearlyPlan));
    }

    @Test
    void theMonthlyChargeIsTheTemplatesPriceNotThePlansListPrice() {
        priceThisTemplate("20.00", "200.00", PlanCode.PREMIUM);

        MockPayment payment = service.checkout(websiteId, owner, new CheckoutRequest(BillingPeriod.MONTHLY));

        assertThat(payment.getAmount()).isEqualByComparingTo("20.00");
    }

    @Test
    void theYearlyChargeIsTheTemplatesYearlyPrice() {
        priceThisTemplate("20.00", "200.00", PlanCode.PREMIUM);

        MockPayment payment = service.checkout(websiteId, owner, new CheckoutRequest(BillingPeriod.YEARLY));

        assertThat(payment.getAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    void twoTemplatesInTheSameFamilyCanCostDifferentAmounts() {
        priceThisTemplate("10.00", "100.00", PlanCode.BASIC);
        assertThat(service.checkout(websiteId, owner, new CheckoutRequest(BillingPeriod.MONTHLY)).getAmount())
                .isEqualByComparingTo("10.00");

        // The same menu family, a different template, a different price.
        website.setLayoutVariant(LayoutVariant.MENU_BISTRO);
        TemplatePrice dearer = new TemplatePrice();
        dearer.setLayoutVariant(LayoutVariant.MENU_BISTRO);
        dearer.setMonthlyPrice(new BigDecimal("20.00"));
        dearer.setYearlyPrice(new BigDecimal("200.00"));
        dearer.setPlanCode(PlanCode.BASIC);
        dearer.setActive(true);
        when(templatePriceRepository.findByLayoutVariant(LayoutVariant.MENU_BISTRO)).thenReturn(Optional.of(dearer));

        assertThat(service.checkout(websiteId, owner, new CheckoutRequest(BillingPeriod.MONTHLY)).getAmount())
                .isEqualByComparingTo("20.00");
    }

    @Test
    void theTemplateAlsoDecidesWhichPlansLimitsTheWebsiteGets() {
        priceThisTemplate("20.00", "200.00", PlanCode.PREMIUM);

        MockPayment payment = service.checkout(websiteId, owner, new CheckoutRequest(BillingPeriod.MONTHLY));

        // The owner never picked PREMIUM; the template they chose carries it, so
        // the limits follow the template just as the price does.
        assertThat(payment.getSubscription().getPlan().getCode()).isEqualTo(PlanCode.PREMIUM);
    }

    @Test
    void anUnpricedTemplateCannotBeCheckedOutRatherThanBeingFree() {
        when(templatePriceRepository.findByLayoutVariant(LayoutVariant.MENU_GRID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkout(websiteId, owner, new CheckoutRequest(BillingPeriod.MONTHLY)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no price set");
    }

    @Test
    void switchingBillingPeriodIsRefusedWhileAPaidSubscriptionRuns() {
        priceThisTemplate("20.00", "200.00", PlanCode.BASIC);
        Plan monthly = new Plan();
        monthly.setCode(PlanCode.BASIC);
        monthly.setBillingPeriod(BillingPeriod.MONTHLY);
        Subscription running = TestEntities.withId(new Subscription(), UUID.randomUUID());
        running.setStatus(SubscriptionStatus.ACTIVE);
        running.setPlan(monthly);
        when(subscriptionRepository.findByWebsiteId(websiteId)).thenReturn(Optional.of(running));

        assertThatThrownBy(() -> service.checkout(websiteId, owner, new CheckoutRequest(BillingPeriod.YEARLY)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Monthly and yearly");
    }

    @Test
    void renewingTheSameBillingPeriodIsAllowed() {
        priceThisTemplate("20.00", "200.00", PlanCode.BASIC);
        Plan monthly = new Plan();
        monthly.setCode(PlanCode.BASIC);
        monthly.setBillingPeriod(BillingPeriod.MONTHLY);
        Subscription running = TestEntities.withId(new Subscription(), UUID.randomUUID());
        running.setStatus(SubscriptionStatus.ACTIVE);
        running.setPlan(monthly);
        when(subscriptionRepository.findByWebsiteId(websiteId)).thenReturn(Optional.of(running));

        assertThat(service.checkout(websiteId, owner, new CheckoutRequest(BillingPeriod.MONTHLY)).getAmount())
                .isEqualByComparingTo("20.00");
    }
}
