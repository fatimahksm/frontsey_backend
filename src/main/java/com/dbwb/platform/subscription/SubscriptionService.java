package com.dbwb.platform.subscription;

import com.dbwb.platform.audit.AuditService;
import com.dbwb.platform.common.config.BusinessRuleProperties;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.notification.EmailService;
import com.dbwb.platform.plan.entity.BillingPeriod;
import com.dbwb.platform.plan.entity.Plan;
import com.dbwb.platform.plan.entity.PlanCode;
import com.dbwb.platform.plan.repository.PlanRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.subscription.dto.CheckoutRequest;
import com.dbwb.platform.subscription.entity.MockPayment;
import com.dbwb.platform.subscription.entity.MockPaymentStatus;
import com.dbwb.platform.subscription.entity.Subscription;
import com.dbwb.platform.subscription.entity.SubscriptionStatus;
import com.dbwb.platform.subscription.repository.MockPaymentRepository;
import com.dbwb.platform.subscription.repository.SubscriptionRepository;
import com.dbwb.platform.website.WebsiteAccessGuard;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.WebsiteStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Section 7.2 + 9.13: subscription lifecycle around the mock Whish payment.
 * BR-SUB-002/010: a subscription may be created (PENDING) before payment,
 * plan changes are only permitted once the current subscription ends, and
 * Publish is blocked until a payment succeeds (see SubscriptionQueryService).
 */
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final MockPaymentRepository mockPaymentRepository;
    private final PlanRepository planRepository;
    private final WebsiteAccessGuard accessGuard;
    private final BusinessRuleProperties businessRules;
    private final EmailService emailService;
    private final AuditService auditService;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            MockPaymentRepository mockPaymentRepository,
            PlanRepository planRepository,
            WebsiteAccessGuard accessGuard,
            BusinessRuleProperties businessRules,
            EmailService emailService,
            AuditService auditService) {
        this.subscriptionRepository = subscriptionRepository;
        this.mockPaymentRepository = mockPaymentRepository;
        this.planRepository = planRepository;
        this.accessGuard = accessGuard;
        this.businessRules = businessRules;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    /** BR-SUB-001/002/010: start (or restart) checkout for a website - owner-only action. */
    @Transactional
    public MockPayment checkout(UUID websiteId, AuthenticatedAccount caller, CheckoutRequest request) {
        BusinessWebsite website = accessGuard.requireOwner(websiteId, caller);

        Plan plan = planRepository.findByCodeAndBillingPeriod(request.planCode(), request.billingPeriod())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found."));

        Subscription subscription = subscriptionRepository.findByWebsiteId(websiteId).orElseGet(Subscription::new);

        // BR-SUB-010: plan change only permitted once the current subscription has ended.
        // A trial is deliberately not "still active" here: its whole purpose is to be
        // converted into a paid plan of the owner's choosing, at any point during it.
        boolean stillActive = subscription.getStatus() == SubscriptionStatus.ACTIVE
                || subscription.getStatus() == SubscriptionStatus.GRACE;
        if (subscription.getId() != null && stillActive && subscription.getPlan() != null
                && subscription.getPlan().getCode() != plan.getCode()) {
            throw new BusinessRuleViolationException(
                    "The plan can only be changed after the current subscription ends.");
        }

        // A trial that is still running stays running until the payment lands.
        // Flipping it to PENDING here would unpublish the site mid-checkout.
        boolean onLiveTrial = subscription.getStatus() == SubscriptionStatus.TRIAL
                && subscription.getEndDate() != null && subscription.getEndDate().isAfter(Instant.now());

        subscription.setWebsite(website);
        subscription.setPlan(plan);
        if (!onLiveTrial) {
            subscription.setStatus(SubscriptionStatus.PENDING);
        }
        subscriptionRepository.save(subscription);

        MockPayment payment = new MockPayment();
        payment.setSubscription(subscription);
        payment.setAmount(plan.getPrice());
        payment.setStatus(MockPaymentStatus.PENDING);
        payment.setReference("MOCK-WHISH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        return mockPaymentRepository.save(payment);
    }

    /**
     * Simulates the mock Whish gateway calling back with an outcome.
     * BR-SUB-004/005: Success activates the subscription automatically (no
     * Super Admin approval needed) and generates a dashboard + email receipt.
     */
    @Transactional
    public MockPayment resolvePaymentOutcome(UUID paymentId, MockPaymentStatus outcome) {
        MockPayment payment = mockPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found."));
        payment.setStatus(outcome);

        if (outcome == MockPaymentStatus.SUCCESS) {
            Subscription subscription = payment.getSubscription();
            Plan plan = subscription.getPlan();

            Instant now = Instant.now();
            // BR-SUB-009: renewal before expiry extends from the existing end date;
            // otherwise (first activation / already expired) it starts from now.
            // Converting from a trial also starts from now - the unused days of a
            // free trial are not credit to be stacked on top of a paid month.
            boolean fromTrial = subscription.getStatus() == SubscriptionStatus.TRIAL;
            Instant base = (!fromTrial && subscription.getEndDate() != null && subscription.getEndDate().isAfter(now))
                    ? subscription.getEndDate() : now;

            Period period = plan.getBillingPeriod() == com.dbwb.platform.plan.entity.BillingPeriod.YEARLY
                    ? Period.ofYears(1) : Period.ofMonths(1);

            Instant newEnd = base.atZone(java.time.ZoneOffset.UTC).plus(period).toInstant();

            subscription.setStartDate(subscription.getStartDate() == null || fromTrial ? now : subscription.getStartDate());
            subscription.setEndDate(newEnd);
            subscription.setGraceEndsAt(newEnd.plus(
                    businessRules.getSubscriptionGracePeriodDays(), ChronoUnit.DAYS));
            subscription.setStatus(SubscriptionStatus.ACTIVE);

            String ownerEmail = subscription.getWebsite().getOwner().getEmail();
            emailService.send(ownerEmail, "Payment receipt",
                    "Your " + plan.getCode() + " (" + plan.getBillingPeriod() + ") subscription is now active. "
                            + "Amount: " + payment.getAmount() + ". Reference: " + payment.getReference());

            auditService.record(subscription.getWebsite().getOwner().getId(), "SUBSCRIPTION_ACTIVATED",
                    subscription.getWebsite().getId().toString());
        }
        return payment;
    }

    /**
     * Opens the free trial for a website that has never had a subscription.
     *
     * Called from the publish path rather than from website creation: the thing
     * being trialled is a working public link, and that does not exist until
     * the owner publishes. Starting the clock at creation would burn the trial
     * while somebody was still typing their menu in.
     *
     * Returns the trial, or empty when the website already has a subscription
     * of any kind - a trial is once per website, and never replaces a paid one.
     */
    @Transactional
    public Optional<Subscription> startTrialIfEligible(BusinessWebsite website) {
        if (subscriptionRepository.findByWebsiteId(website.getId()).isPresent()) {
            return Optional.empty();
        }

        Plan plan = planRepository.findByCodeAndBillingPeriod(PlanCode.BASIC, BillingPeriod.MONTHLY)
                .orElseThrow(() -> new ResourceNotFoundException("No BASIC plan configured to base a trial on."));

        Instant now = Instant.now();
        Subscription trial = new Subscription();
        trial.setWebsite(website);
        trial.setPlan(plan);
        trial.setStatus(SubscriptionStatus.TRIAL);
        trial.setStartDate(now);
        trial.setEndDate(now.plus(businessRules.getSubscriptionTrialDays(), ChronoUnit.DAYS));
        // Deliberately no graceEndsAt: a trial ends on its end date, full stop.
        trial.setGraceEndsAt(null);
        subscriptionRepository.save(trial);

        emailService.send(website.getOwner().getEmail(), "Your free trial has started",
                "Your website is live for the next " + businessRules.getSubscriptionTrialDays()
                        + " days. Subscribe before it ends to keep it online.");
        auditService.record(website.getOwner().getId(), "SUBSCRIPTION_TRIAL_STARTED", website.getId().toString());
        return Optional.of(trial);
    }

    @Transactional(readOnly = true)
    public Subscription get(UUID websiteId, AuthenticatedAccount caller) {
        accessGuard.requireReadAccess(websiteId, caller);
        return subscriptionRepository.findByWebsiteIdWithPlan(websiteId)
                .orElseThrow(() -> new ResourceNotFoundException("No subscription found for this website."));
    }

    /**
     * Scheduled maintenance (called by SubscriptionMaintenanceJob):
     * BR-SUB-006/007/008 - move ACTIVE subscriptions past endDate into GRACE,
     * and GRACE subscriptions past graceEndsAt into EXPIRED, stopping the
     * public site (BR-SUB-008 / website status EXPIRED).
     */
    @Transactional
    public void runLifecycleMaintenance() {
        Instant now = Instant.now();

        // A finished trial stops the site the same day. There is no grace period
        // on something that was never paid for - that is what makes it a trial.
        subscriptionRepository.findByStatusAndEndDateBefore(SubscriptionStatus.TRIAL, now)
                .forEach(sub -> {
                    sub.setStatus(SubscriptionStatus.EXPIRED);
                    BusinessWebsite website = sub.getWebsite();
                    website.setStatus(WebsiteStatus.EXPIRED);
                    emailService.send(website.getOwner().getEmail(), "Your free trial has ended",
                            "Your trial is over and your website is no longer publicly available. "
                                    + "Subscribe to bring it back online.");
                });

        subscriptionRepository.findByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, now)
                .forEach(sub -> {
                    sub.setStatus(SubscriptionStatus.GRACE);
                    emailService.send(sub.getWebsite().getOwner().getEmail(), "Subscription entering grace period",
                            "Your subscription has expired and is now in a "
                                    + businessRules.getSubscriptionGracePeriodDays() + "-day grace period.");
                });

        subscriptionRepository.findByStatusAndGraceEndsAtBefore(SubscriptionStatus.GRACE, now)
                .forEach(sub -> {
                    sub.setStatus(SubscriptionStatus.EXPIRED);
                    BusinessWebsite website = sub.getWebsite();
                    website.setStatus(WebsiteStatus.EXPIRED); // BR-SUB-008: public website stops immediately
                    emailService.send(website.getOwner().getEmail(), "Website unpublished - subscription expired",
                            "Your grace period has ended and your website is no longer publicly available.");
                });
    }
}
