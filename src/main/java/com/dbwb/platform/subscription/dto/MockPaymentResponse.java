package com.dbwb.platform.subscription.dto;

import com.dbwb.platform.subscription.entity.MockPayment;
import com.dbwb.platform.subscription.entity.MockPaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record MockPaymentResponse(UUID id, UUID subscriptionId, BigDecimal amount, MockPaymentStatus status, String reference) {
    public static MockPaymentResponse from(MockPayment p) {
        return new MockPaymentResponse(p.getId(), p.getSubscription().getId(), p.getAmount(), p.getStatus(), p.getReference());
    }
}
