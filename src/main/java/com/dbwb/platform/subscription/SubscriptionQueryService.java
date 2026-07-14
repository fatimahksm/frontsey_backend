package com.dbwb.platform.subscription;

import com.dbwb.platform.subscription.entity.SubscriptionStatus;
import com.dbwb.platform.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * BR-RULE-001: a website cannot be published without an active subscription
 * or a valid grace-period state. Kept as a small, focused read path so the
 * website module doesn't need the full SubscriptionService (which also
 * handles the mock payment flow) just to check publishability.
 */
@Service
public class SubscriptionQueryService {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionQueryService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public boolean hasPublishableSubscription(UUID websiteId) {
        return subscriptionRepository.findByWebsiteId(websiteId)
                .map(sub -> sub.getStatus() == SubscriptionStatus.ACTIVE || sub.getStatus() == SubscriptionStatus.GRACE)
                .orElse(false);
    }
}
