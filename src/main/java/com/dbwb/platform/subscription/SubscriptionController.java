package com.dbwb.platform.subscription;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.security.CurrentAccount;
import com.dbwb.platform.subscription.dto.CheckoutRequest;
import com.dbwb.platform.subscription.dto.MockPaymentResponse;
import com.dbwb.platform.subscription.dto.SubscriptionResponse;
import com.dbwb.platform.subscription.entity.MockPaymentStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CurrentAccount currentAccount;

    public SubscriptionController(SubscriptionService subscriptionService, CurrentAccount currentAccount) {
        this.subscriptionService = subscriptionService;
        this.currentAccount = currentAccount;
    }

    @GetMapping("/api/websites/{websiteId}/subscription")
    public ApiResponse<SubscriptionResponse> get(@PathVariable UUID websiteId) {
        return ApiResponse.ok(SubscriptionResponse.from(subscriptionService.get(websiteId, currentAccount.get())));
    }

    @PostMapping("/api/websites/{websiteId}/subscription/checkout")
    public ApiResponse<MockPaymentResponse> checkout(@PathVariable UUID websiteId, @Valid @RequestBody CheckoutRequest request) {
        var payment = subscriptionService.checkout(websiteId, currentAccount.get(), request);
        return ApiResponse.ok(MockPaymentResponse.from(payment),
                "Mock Whish checkout created. Simulate an outcome to continue.");
    }

    /**
     * Simulates the mock Whish gateway's callback. In a real integration this
     * would be a webhook from Whish; here the frontend calls it directly to
     * emulate Success/Failed/Pending during the MVP (BR-SUB-003).
     */
    @PostMapping("/api/payments/mock/{paymentId}/simulate")
    public ApiResponse<MockPaymentResponse> simulateOutcome(@PathVariable UUID paymentId, @RequestParam MockPaymentStatus outcome) {
        var payment = subscriptionService.resolvePaymentOutcome(paymentId, outcome);
        return ApiResponse.ok(MockPaymentResponse.from(payment));
    }
}
