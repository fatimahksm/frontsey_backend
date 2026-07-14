package com.dbwb.platform.website;

import com.dbwb.platform.website.repository.BusinessWebsiteRepository;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.regex.Pattern;

/** BR-RULE-014: website slugs are generated automatically and must remain unique. */
@Component
public class SlugGenerator {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    private final BusinessWebsiteRepository websiteRepository;

    public SlugGenerator(BusinessWebsiteRepository websiteRepository) {
        this.websiteRepository = websiteRepository;
    }

    public String generateUniqueSlug(String businessName) {
        String base = normalize(businessName);
        String candidate = base;
        int suffix = 1;
        while (websiteRepository.existsBySlug(candidate)) {
            suffix++;
            candidate = base + "-" + suffix;
        }
        return candidate;
    }

    private String normalize(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "") // strip diacritics
                .toLowerCase();
        String slug = NON_ALPHANUMERIC.matcher(normalized).replaceAll("-");
        slug = slug.replaceAll("^-+|-+$", "");
        return slug.isBlank() ? "business" : slug;
    }
}
