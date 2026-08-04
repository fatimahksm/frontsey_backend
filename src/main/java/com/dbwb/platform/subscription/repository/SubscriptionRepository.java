package com.dbwb.platform.subscription.repository;

import com.dbwb.platform.subscription.entity.Subscription;
import com.dbwb.platform.subscription.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByWebsiteId(UUID websiteId);

    /**
     * Plan is a lazy @ManyToOne; SubscriptionResponse.from() maps
     * subscription.getPlan().getCode()/.getBillingPeriod() in the controller,
     * after SubscriptionService's read-only transaction has already closed -
     * same class of bug as BusinessWebsiteRepository.findAllWithOwner().
     */
    @Query("select s from Subscription s join fetch s.plan where s.website.id = :websiteId")
    Optional<Subscription> findByWebsiteIdWithPlan(UUID websiteId);
    List<Subscription> findByStatusAndEndDateBefore(SubscriptionStatus status, Instant instant);
    List<Subscription> findByStatusAndGraceEndsAtBefore(SubscriptionStatus status, Instant instant);
    long countByStatus(SubscriptionStatus status);
    List<Subscription> findByStatusAndEndDateBetween(SubscriptionStatus status, Instant from, Instant to);
}
