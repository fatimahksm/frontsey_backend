package com.dbwb.platform.plan;

import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.plan.entity.TemplatePrice;
import com.dbwb.platform.plan.repository.TemplatePriceRepository;
import com.dbwb.platform.website.entity.LayoutVariant;
import com.dbwb.platform.website.entity.TemplateType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Which templates a Super Admin currently offers, and the one place that
 * question is answered.
 *
 * template_prices.active was already the switch, but it only ever reached
 * pricing: the picker is a fixed list in the frontend, and nothing on the way
 * in checked it, so turning a template off left it visible, selectable and
 * creatable - the owner only met the wall at checkout, after choosing.
 *
 * "Off" means not offered to anyone choosing now. It deliberately does not mean
 * the template stops working: a website already on it keeps rendering, keeps
 * publishing, and keeps being able to pay. Withdrawing a template from sale
 * must never strand the customers already on it.
 */
@Component
public class TemplateAvailability {

    private final TemplatePriceRepository templatePriceRepository;

    public TemplateAvailability(TemplatePriceRepository templatePriceRepository) {
        this.templatePriceRepository = templatePriceRepository;
    }

    /** The templates on offer, in enum order. */
    public List<TemplatePrice> offered() {
        return templatePriceRepository.findAllByActiveTrueOrderByLayoutVariantAsc();
    }

    public Set<LayoutVariant> offeredVariants() {
        return offered().stream().map(TemplatePrice::getLayoutVariant).collect(Collectors.toSet());
    }

    /**
     * A template with no price row at all counts as not offered. That is the
     * safer reading: an unpriced template is one nobody can be charged for, so
     * letting a website onto it only produces an unpayable site later.
     */
    public boolean isOffered(LayoutVariant layoutVariant) {
        return templatePriceRepository.findByLayoutVariant(layoutVariant)
                .filter(TemplatePrice::isActive)
                .isPresent();
    }

    public boolean hasAnyOffered(TemplateType templateType) {
        return offeredVariants().stream().anyMatch(variant -> variant.templateType() == templateType);
    }

    /** Refuses a template that is not on offer, in words an owner can act on. */
    public void requireOffered(LayoutVariant layoutVariant) {
        if (!isOffered(layoutVariant)) {
            throw new BusinessRuleViolationException(
                    "That template is not available to choose right now. Pick another one.");
        }
    }

    public void requireAnyOffered(TemplateType templateType) {
        if (!hasAnyOffered(templateType)) {
            throw new BusinessRuleViolationException(
                    "No templates of that kind are available right now. Try a different kind of website.");
        }
    }
}
