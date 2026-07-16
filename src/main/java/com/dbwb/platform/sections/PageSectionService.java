package com.dbwb.platform.sections;

import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.sections.dto.PageSectionRequest;
import com.dbwb.platform.sections.entity.PageSection;
import com.dbwb.platform.sections.repository.PageSectionRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.website.WebsiteAccessGuard;
import com.dbwb.platform.website.entity.BusinessWebsite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD + reordering for owner-added page sections. Available on both
 * template types (Menu and Portfolio) - reuses MANAGE_THEME_AND_CONTENT since
 * these are presentational additions to the page, same grant as layout and
 * hero copy/brand color.
 */
@Service
public class PageSectionService {

    private final WebsiteAccessGuard accessGuard;
    private final PageSectionRepository repository;

    public PageSectionService(WebsiteAccessGuard accessGuard, PageSectionRepository repository) {
        this.accessGuard = accessGuard;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PageSection> list(UUID websiteId, AuthenticatedAccount caller) {
        accessGuard.requireReadAccess(websiteId, caller);
        return repository.findByWebsiteIdOrderBySortOrder(websiteId);
    }

    @Transactional
    public PageSection create(UUID websiteId, AuthenticatedAccount caller, PageSectionRequest request) {
        BusinessWebsite website = accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);

        PageSection section = new PageSection();
        section.setWebsite(website);
        section.setSortOrder((int) repository.countByWebsiteId(websiteId));
        applyRequest(section, request);
        return repository.save(section);
    }

    @Transactional
    public PageSection update(UUID websiteId, UUID sectionId, AuthenticatedAccount caller, PageSectionRequest request) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        PageSection section = load(sectionId, websiteId);
        applyRequest(section, request);
        return section;
    }

    @Transactional
    public void delete(UUID websiteId, UUID sectionId, AuthenticatedAccount caller) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        repository.delete(load(sectionId, websiteId));
    }

    /** Reorders the whole list - sectionIds must be the complete, ordered set for this website. */
    @Transactional
    public void reorder(UUID websiteId, AuthenticatedAccount caller, List<UUID> sectionIds) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        for (int i = 0; i < sectionIds.size(); i++) {
            load(sectionIds.get(i), websiteId).setSortOrder(i);
        }
    }

    private void applyRequest(PageSection section, PageSectionRequest request) {
        section.setType(request.type());
        section.setData(request.data());
    }

    private PageSection load(UUID sectionId, UUID websiteId) {
        PageSection section = repository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found."));
        if (!section.getWebsite().getId().equals(websiteId)) {
            throw new ResourceNotFoundException("Section not found.");
        }
        return section;
    }
}
