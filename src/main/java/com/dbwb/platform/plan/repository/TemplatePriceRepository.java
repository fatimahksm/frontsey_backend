package com.dbwb.platform.plan.repository;

import com.dbwb.platform.plan.entity.TemplatePrice;
import com.dbwb.platform.website.entity.LayoutVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplatePriceRepository extends JpaRepository<TemplatePrice, UUID> {
    Optional<TemplatePrice> findByLayoutVariant(LayoutVariant layoutVariant);

    List<TemplatePrice> findAllByOrderByLayoutVariantAsc();

    /** Only the templates currently on offer - what a picker may show. */
    List<TemplatePrice> findAllByActiveTrueOrderByLayoutVariantAsc();
}
