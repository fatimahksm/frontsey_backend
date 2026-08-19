package com.dbwb.platform.portfolio;

import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.portfolio.dto.PortfolioProjectRequest;
import com.dbwb.platform.portfolio.entity.PortfolioProject;
import com.dbwb.platform.portfolio.repository.PortfolioProjectRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.website.WebsiteAccessGuard;
import com.dbwb.platform.website.entity.BusinessWebsite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Projects on a PORTFOLIO website. Gated on MANAGE_THEME_AND_CONTENT, the same
 * permission as the gallery and custom sections - a project is website content,
 * not a catalogue entry.
 */
@Service
public class PortfolioProjectService {

    private final PortfolioProjectRepository repository;
    private final WebsiteAccessGuard accessGuard;

    public PortfolioProjectService(PortfolioProjectRepository repository, WebsiteAccessGuard accessGuard) {
        this.repository = repository;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public List<PortfolioProject> list(UUID websiteId, AuthenticatedAccount caller) {
        accessGuard.requireReadAccess(websiteId, caller);
        return repository.findByWebsiteIdOrderBySortOrder(websiteId);
    }

    @Transactional
    public PortfolioProject create(UUID websiteId, AuthenticatedAccount caller, PortfolioProjectRequest request) {
        BusinessWebsite website = accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        PortfolioProject project = new PortfolioProject();
        project.setWebsite(website);
        apply(project, request);
        // Appended rather than inserted: a new project goes to the end of the
        // owner's existing order instead of silently reshuffling it.
        project.setSortOrder(repository.findByWebsiteIdOrderBySortOrder(websiteId).size());
        return repository.save(project);
    }

    @Transactional
    public PortfolioProject update(UUID websiteId, UUID projectId, AuthenticatedAccount caller, PortfolioProjectRequest request) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        PortfolioProject project = requireOwned(websiteId, projectId);
        apply(project, request);
        return project;
    }

    @Transactional
    public void delete(UUID websiteId, UUID projectId, AuthenticatedAccount caller) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        repository.delete(requireOwned(websiteId, projectId));
    }

    /**
     * Rewrites the display order from the given id sequence. Ids that do not
     * belong to this website are ignored rather than rejected, so a stale tab
     * reordering a list cannot move another site's rows.
     */
    @Transactional
    public List<PortfolioProject> reorder(UUID websiteId, AuthenticatedAccount caller, List<UUID> orderedIds) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        List<PortfolioProject> projects = repository.findByWebsiteIdOrderBySortOrder(websiteId);
        int position = 0;
        for (UUID id : orderedIds) {
            for (PortfolioProject project : projects) {
                if (project.getId().equals(id)) {
                    project.setSortOrder(position++);
                    break;
                }
            }
        }
        return repository.findByWebsiteIdOrderBySortOrder(websiteId);
    }

    /**
     * A project id alone is not enough: without checking it belongs to this
     * website, a caller with rights to their own site could edit anyone's
     * project by guessing an id.
     */
    private PortfolioProject requireOwned(UUID websiteId, UUID projectId) {
        PortfolioProject project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
        if (!project.getWebsite().getId().equals(websiteId)) {
            throw new ResourceNotFoundException("Project not found.");
        }
        return project;
    }

    private void apply(PortfolioProject project, PortfolioProjectRequest request) {
        project.setName(request.name());
        project.setDiscipline(request.discipline());
        project.setYear(request.year());
        project.setSummary(request.summary());
        project.setTags(request.tags());
        project.setImageUrl(request.imageUrl());
        project.setLiveUrl(request.liveUrl());
        project.setRepoUrl(request.repoUrl());
    }
}
