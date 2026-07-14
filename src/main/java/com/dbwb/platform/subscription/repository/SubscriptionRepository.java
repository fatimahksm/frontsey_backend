package com.dbwb.platform.subscription.repository;

import com.dbwb.platform.subscription.entity.Subscription;
import com.dbwb.platform.subscription.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByWebsiteId(UUID websiteId);
    List<Subscription> findByStatusAndEndDateBefore(SubscriptionStatus status, Instant instant);
    List<Subscription> findByStatusAndGraceEndsAtBefore(SubscriptionStatus status, Instant instant);
}
