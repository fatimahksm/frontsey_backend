package com.dbwb.platform.portfolio;

import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.portfolio.dto.PortfolioProjectRequest;
import com.dbwb.platform.portfolio.entity.PortfolioProject;
import com.dbwb.platform.portfolio.repository.PortfolioProjectRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.testsupport.TestEntities;
import com.dbwb.platform.website.WebsiteAccessGuard;
import com.dbwb.platform.website.entity.BusinessWebsite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The tests PORTFOLIO-PROJECTS-PROGRESS.md listed as missing. The suite passed
 * without them only because nothing exercised this service at all - and, as
 * PortfolioProjectRepositoryTest documents, the table it writes to did not
 * even exist in the test schema.
 */
@ExtendWith(MockitoExtension.class)
class PortfolioProjectServiceTest {

    @Mock
    private PortfolioProjectRepository repository;
    @Mock
    private WebsiteAccessGuard accessGuard;

    private PortfolioProjectService projectService;

    private final UUID websiteId = UUID.randomUUID();
    private final AuthenticatedAccount caller =
            new AuthenticatedAccount(UUID.randomUUID(), "owner@example.com", Role.BUSINESS_OWNER);
    private BusinessWebsite website;

    @BeforeEach
    void setUp() {
        projectService = new PortfolioProjectService(repository, accessGuard);
        website = TestEntities.withId(new BusinessWebsite(), websiteId);
        lenient().when(accessGuard.requirePermission(eq(websiteId), eq(caller), any())).thenReturn(website);
    }

    @Test
    void createAppendsToTheEndOfTheOwnersExistingOrder() {
        when(repository.findByWebsiteIdOrderBySortOrder(websiteId))
                .thenReturn(List.of(projectWithId("First", 0), projectWithId("Second", 1)));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var created = projectService.create(websiteId, caller, request("Third"));

        assertThat(created.getSortOrder()).isEqualTo(2);
        assertThat(created.getName()).isEqualTo("Third");
        assertThat(created.getWebsite()).isSameAs(website);
    }

    @Test
    void createIsGatedOnManagingContent() {
        when(repository.findByWebsiteIdOrderBySortOrder(websiteId)).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        projectService.create(websiteId, caller, request("Only"));

        verify(accessGuard).requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
    }

    @Test
    void updateRejectsAProjectBelongingToAnotherWebsite() {
        UUID projectId = UUID.randomUUID();
        PortfolioProject foreign = new PortfolioProject();
        foreign.setWebsite(TestEntities.withId(new BusinessWebsite(), UUID.randomUUID()));
        when(repository.findById(projectId)).thenReturn(Optional.of(TestEntities.withId(foreign, projectId)));

        assertThatThrownBy(() -> projectService.update(websiteId, projectId, caller, request("Renamed")))
                .isInstanceOf(ResourceNotFoundException.class);

        // The point of the check: the foreign row must be left exactly as it was.
        assertThat(foreign.getName()).isNull();
    }

    @Test
    void deleteRejectsAProjectBelongingToAnotherWebsite() {
        UUID projectId = UUID.randomUUID();
        PortfolioProject foreign = new PortfolioProject();
        foreign.setWebsite(TestEntities.withId(new BusinessWebsite(), UUID.randomUUID()));
        when(repository.findById(projectId)).thenReturn(Optional.of(TestEntities.withId(foreign, projectId)));

        assertThatThrownBy(() -> projectService.delete(websiteId, projectId, caller))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).delete(any());
    }

    @Test
    void updateAppliesEveryEditableFieldIncludingYear() {
        UUID projectId = UUID.randomUUID();
        PortfolioProject mine = TestEntities.withId(projectWithId("Old name", 0), projectId);
        when(repository.findById(projectId)).thenReturn(Optional.of(mine));

        projectService.update(websiteId, projectId, caller, new PortfolioProjectRequest(
                "New name", "Frontend", "2025", "A summary", "react,typescript",
                "https://example.com/i.png", "https://example.com/live", "https://example.com/repo"));

        assertThat(mine.getName()).isEqualTo("New name");
        assertThat(mine.getYear()).isEqualTo("2025");
        assertThat(mine.getTags()).isEqualTo("react,typescript");
        assertThat(mine.getRepoUrl()).isEqualTo("https://example.com/repo");
        // Not part of the request, so reordering is never a side effect of an edit.
        assertThat(mine.getSortOrder()).isZero();
    }

    @Test
    void reorderRewritesSortOrderFromTheGivenSequence() {
        PortfolioProject first = projectWithId("First", 0);
        PortfolioProject second = projectWithId("Second", 1);
        PortfolioProject third = projectWithId("Third", 2);
        when(repository.findByWebsiteIdOrderBySortOrder(websiteId)).thenReturn(List.of(first, second, third));

        projectService.reorder(websiteId, caller, List.of(third.getId(), first.getId(), second.getId()));

        assertThat(third.getSortOrder()).isZero();
        assertThat(first.getSortOrder()).isEqualTo(1);
        assertThat(second.getSortOrder()).isEqualTo(2);
    }

    @Test
    void reorderIgnoresIdsThatBelongToAnotherWebsite() {
        PortfolioProject first = projectWithId("First", 0);
        PortfolioProject second = projectWithId("Second", 1);
        when(repository.findByWebsiteIdOrderBySortOrder(websiteId)).thenReturn(List.of(first, second));

        // A stale tab submitting someone else's id must not shift this site's rows.
        projectService.reorder(websiteId, caller, List.of(UUID.randomUUID(), second.getId(), first.getId()));

        assertThat(second.getSortOrder()).isZero();
        assertThat(first.getSortOrder()).isEqualTo(1);
    }

    private PortfolioProjectRequest request(String name) {
        return new PortfolioProjectRequest(name, null, null, null, null, null, null, null);
    }

    private PortfolioProject projectWithId(String name, int sortOrder) {
        PortfolioProject project = new PortfolioProject();
        project.setWebsite(website);
        project.setName(name);
        project.setSortOrder(sortOrder);
        return TestEntities.withId(project, UUID.randomUUID());
    }
}
