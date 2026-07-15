package com.dbwb.platform.website;

import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.website.dto.SeoMetadataRequest;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.SeoMetadata;
import com.dbwb.platform.website.repository.SeoMetadataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** BRD 9.11: per-website SEO metadata, editable alongside theme/content. */
@Service
public class SeoMetadataService {

    private final SeoMetadataRepository repository;
    private final WebsiteAccessGuard accessGuard;

    public SeoMetadataService(SeoMetadataRepository repository, WebsiteAccessGuard accessGuard) {
        this.repository = repository;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public Optional<SeoMetadata> get(UUID websiteId, AuthenticatedAccount caller) {
        accessGuard.requireReadAccess(websiteId, caller);
        return repository.findByWebsiteId(websiteId);
    }

    @Transactional
    public SeoMetadata update(UUID websiteId, AuthenticatedAccount caller, SeoMetadataRequest request) {
        BusinessWebsite website = accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        SeoMetadata seo = repository.findByWebsiteId(websiteId).orElseGet(() -> {
            SeoMetadata created = new SeoMetadata();
            created.setWebsite(website);
            return created;
        });

        seo.setMetaTitle(request.metaTitle());
        seo.setMetaDescription(request.metaDescription());
        seo.setOgImageUrl(request.ogImageUrl());
        repository.save(seo);
        return seo;
    }
}
