package com.dbwb.platform.plan;

import com.dbwb.platform.common.config.BusinessRuleProperties;
import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.plan.dto.TemplatePriceResponse;
import com.dbwb.platform.plan.repository.TemplatePriceRepository;
import com.dbwb.platform.website.entity.LayoutVariant;
import org.springframework.web.bind.annotation.PathVariable;
import com.dbwb.platform.plan.dto.PlanResponse;
import com.dbwb.platform.plan.repository.PlanRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only pricing listing (7.2) for the pricing page / checkout screen. Plan editing is a Super Admin action (BR-ADM-007). */
@RestController
@RequestMapping("/api/public/plans")
public class PlanController {

    private final PlanRepository planRepository;
    private final TemplatePriceRepository templatePriceRepository;
    private final BusinessRuleProperties businessRules;
    private final TemplateAvailability templateAvailability;

    public PlanController(PlanRepository planRepository, TemplatePriceRepository templatePriceRepository,
                          BusinessRuleProperties businessRules,
                          TemplateAvailability templateAvailability) {
        this.planRepository = planRepository;
        this.templatePriceRepository = templatePriceRepository;
        this.businessRules = businessRules;
        this.templateAvailability = templateAvailability;
    }

    @GetMapping
    public ApiResponse<List<PlanResponse>> list() {
        return ApiResponse.ok(planRepository.findByActiveTrue().stream().map(PlanResponse::from).toList());
    }

    /**
     * The templates currently on offer - what a picker may show.
     *
     * The frontend's template list is otherwise a fixed array, which is why
     * turning a template off used to leave it on display: it was hidden from
     * pricing and from checkout, but never from the people choosing.
     */
    @GetMapping("/templates")
    public ApiResponse<List<TemplatePriceResponse>> offeredTemplates() {
        return ApiResponse.ok(templateAvailability.offered().stream()
                .map(TemplatePriceResponse::from)
                .toList());
    }

    /**
     * What one template costs. Public because the subscription screen shows it
     * before anybody has committed to anything, and a price list is not private.
     *
     * Deliberately not filtered on active: this answers "what does this website
     * pay", and a website already on a withdrawn template still has to be able
     * to see and settle its bill. Use /templates for what is on offer.
     */
    @GetMapping("/template/{layoutVariant}")
    public ApiResponse<TemplatePriceResponse> templatePrice(@PathVariable LayoutVariant layoutVariant) {
        return ApiResponse.ok(templatePriceRepository.findByLayoutVariant(layoutVariant)
                .map(TemplatePriceResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("That template has no price set.")));
    }

    /**
     * How long the free trial runs, so the dashboard can say "10 days" without
     * keeping its own copy of a number that lives in config.
     */
    @GetMapping("/trial-days")
    public ApiResponse<Integer> trialDays() {
        return ApiResponse.ok(businessRules.getSubscriptionTrialDays());
    }
}
