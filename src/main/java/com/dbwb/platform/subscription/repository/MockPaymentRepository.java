package com.dbwb.platform.subscription.repository;

import com.dbwb.platform.subscription.entity.MockPayment;
import com.dbwb.platform.subscription.entity.MockPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface MockPaymentRepository extends JpaRepository<MockPayment, UUID> {
    List<MockPayment> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId);

    long countByStatus(MockPaymentStatus status);

    @Query("select coalesce(sum(p.amount), 0) from MockPayment p where p.status = 'SUCCESS'")
    BigDecimal totalRevenue();
}
