package com.dbwb.platform.subscription.repository;

import com.dbwb.platform.subscription.entity.MockPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MockPaymentRepository extends JpaRepository<MockPayment, UUID> {
    List<MockPayment> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId);
}
