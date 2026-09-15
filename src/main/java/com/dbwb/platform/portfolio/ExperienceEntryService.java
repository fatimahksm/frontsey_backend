package com.dbwb.platform.portfolio;

import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.portfolio.dto.ExperienceEntryRequest;
import com.dbwb.platform.portfolio.entity.ExperienceEntry;
import com.dbwb.platform.portfolio.repository.ExperienceEntryRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.website.WebsiteAccessGuard;
import com.dbwb.platform.website.entity.BusinessWebsite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * An owner's work history on a PORTFOLIO website. Gated on
 * MANAGE_THEME_AND_CONTENT, the same permission as projects and the gallery -
 * it is website content rather than a catalogue entry.
 */
@Service
public class ExperienceEntryService {

    private final ExperienceEntryRepository repository;
    private final WebsiteAccessGuard accessGuard;

    public ExperienceEntryService(ExperienceEntryRepository repository, WebsiteAccessGuard accessGuard) {
        this.repository = repository;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public List<ExperienceEntry> list(UUID websiteId, AuthenticatedAccount caller) {
        accessGuard.requireReadAccess(websiteId, caller);
        return repository.findByWebsiteIdOrderBySortOrder(websiteId);
    }

    @Transactional
    public ExperienceEntry create(UUID websiteId, AuthenticatedAccount caller, ExperienceEntryRequest request) {
        BusinessWebsite website = accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        ExperienceEntry entry = new ExperienceEntry();
        entry.setWebsite(website);
        apply(entry, request);
        // Appended rather than inserted: a new entry goes to the end of the
        // owner's order instead of silently reshuffling it. A timeline is
        // usually newest-first, which is an order the owner arranges.
        entry.setSortOrder(repository.findByWebsiteIdOrderBySortOrder(websiteId).size());
        return repository.save(entry);
    }

    @Transactional
    public ExperienceEntry update(UUID websiteId, UUID entryId, AuthenticatedAccount caller, ExperienceEntryRequest request) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        ExperienceEntry entry = requireOwned(websiteId, entryId);
        apply(entry, request);
        return entry;
    }

    @Transactional
    public void delete(UUID websiteId, UUID entryId, AuthenticatedAccount caller) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        repository.delete(requireOwned(websiteId, entryId));
    }

    /**
     * Rewrites the display order from the given id sequence. Ids that do not
     * belong to this website are ignored rather than rejected, so a stale tab
     * reordering a list cannot move another site's rows.
     */
    @Transactional
    public List<ExperienceEntry> reorder(UUID websiteId, AuthenticatedAccount caller, List<UUID> orderedIds) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        List<ExperienceEntry> entries = repository.findByWebsiteIdOrderBySortOrder(websiteId);
        int position = 0;
        for (UUID id : orderedIds) {
            for (ExperienceEntry entry : entries) {
                if (entry.getId().equals(id)) {
                    entry.setSortOrder(position++);
                    break;
                }
            }
        }
        return repository.findByWebsiteIdOrderBySortOrder(websiteId);
    }

    /**
     * An entry id alone is not enough: without checking it belongs to this
     * website, a caller with rights to their own site could edit anyone's
     * work history by guessing an id.
     */
    private ExperienceEntry requireOwned(UUID websiteId, UUID entryId) {
        ExperienceEntry entry = repository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Experience entry not found."));
        if (!entry.getWebsite().getId().equals(websiteId)) {
            throw new ResourceNotFoundException("Experience entry not found.");
        }
        return entry;
    }

    private void apply(ExperienceEntry entry, ExperienceEntryRequest request) {
        entry.setRole(request.role());
        entry.setCompany(request.company());
        entry.setYear(request.year());
        entry.setDetail(request.detail());
    }
}
