package com.dbwb.platform.portfolio;

import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.portfolio.dto.ExperienceEntryRequest;
import com.dbwb.platform.portfolio.entity.ExperienceEntry;
import com.dbwb.platform.portfolio.repository.ExperienceEntryRepository;
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
 * The same shape of tests PortfolioProjectServiceTest covers, on the sibling
 * that PORTFOLIO-PROJECTS-PROGRESS.md listed as "not started": the tenant
 * check on every write, the append-to-end order, and a reorder that ignores
 * ids belonging to someone else.
 */
@ExtendWith(MockitoExtension.class)
class ExperienceEntryServiceTest {

    @Mock
    private ExperienceEntryRepository repository;
    @Mock
    private WebsiteAccessGuard accessGuard;

    private ExperienceEntryService experienceService;

    private final UUID websiteId = UUID.randomUUID();
    private final AuthenticatedAccount caller =
            new AuthenticatedAccount(UUID.randomUUID(), "owner@example.com", Role.BUSINESS_OWNER);
    private BusinessWebsite website;

    @BeforeEach
    void setUp() {
        experienceService = new ExperienceEntryService(repository, accessGuard);
        website = TestEntities.withId(new BusinessWebsite(), websiteId);
        lenient().when(accessGuard.requirePermission(eq(websiteId), eq(caller), any())).thenReturn(website);
    }

    @Test
    void createsAnEntryGatedOnContentPermission() {
        when(repository.findByWebsiteIdOrderBySortOrder(websiteId)).thenReturn(List.of());
        when(repository.save(any(ExperienceEntry.class))).thenAnswer(call -> call.getArgument(0));

        ExperienceEntry created = experienceService.create(websiteId, caller,
                new ExperienceEntryRequest("Lead designer", "Studio Beirut", "2021-24", "Brand work."));

        verify(accessGuard).requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        assertThat(created.getRole()).isEqualTo("Lead designer");
        assertThat(created.getCompany()).isEqualTo("Studio Beirut");
        assertThat(created.getWebsite()).isSameAs(website);
    }

    @Test
    void appendsANewEntryToTheEndOfTheOwnersOrder() {
        when(repository.findByWebsiteIdOrderBySortOrder(websiteId))
                .thenReturn(List.of(new ExperienceEntry(), new ExperienceEntry(), new ExperienceEntry()));
        when(repository.save(any(ExperienceEntry.class))).thenAnswer(call -> call.getArgument(0));

        ExperienceEntry created = experienceService.create(websiteId, caller,
                new ExperienceEntryRequest("Freelance", null, null, null));

        // Inserting at the top would silently reshuffle an order the owner set.
        assertThat(created.getSortOrder()).isEqualTo(3);
    }

    /**
     * The check that matters most here. Without it an owner with rights to
     * their own site could rewrite anyone's work history by guessing an id.
     */
    @Test
    void refusesToUpdateAnEntryBelongingToAnotherWebsite() {
        ExperienceEntry theirs = new ExperienceEntry();
        theirs.setWebsite(TestEntities.withId(new BusinessWebsite(), UUID.randomUUID()));
        UUID entryId = UUID.randomUUID();
        when(repository.findById(entryId)).thenReturn(Optional.of(theirs));

        assertThatThrownBy(() -> experienceService.update(websiteId, entryId, caller,
                new ExperienceEntryRequest("Hijacked", null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(theirs.getRole()).isNull();
    }

    @Test
    void refusesToDeleteAnEntryBelongingToAnotherWebsite() {
        ExperienceEntry theirs = new ExperienceEntry();
        theirs.setWebsite(TestEntities.withId(new BusinessWebsite(), UUID.randomUUID()));
        UUID entryId = UUID.randomUUID();
        when(repository.findById(entryId)).thenReturn(Optional.of(theirs));

        assertThatThrownBy(() -> experienceService.delete(websiteId, entryId, caller))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void reorderRewritesTheOrderFromTheGivenSequence() {
        ExperienceEntry first = TestEntities.withId(new ExperienceEntry(), UUID.randomUUID());
        ExperienceEntry second = TestEntities.withId(new ExperienceEntry(), UUID.randomUUID());
        when(repository.findByWebsiteIdOrderBySortOrder(websiteId)).thenReturn(List.of(first, second));

        experienceService.reorder(websiteId, caller, List.of(second.getId(), first.getId()));

        assertThat(second.getSortOrder()).isZero();
        assertThat(first.getSortOrder()).isEqualTo(1);
    }

    /**
     * A stale tab posting a list that includes another site's id must move
     * this site's rows and nothing else - ignored rather than rejected, so one
     * unknown id does not lose the whole reorder.
     */
    @Test
    void reorderIgnoresIdsFromAnotherWebsite() {
        ExperienceEntry mine = TestEntities.withId(new ExperienceEntry(), UUID.randomUUID());
        when(repository.findByWebsiteIdOrderBySortOrder(websiteId)).thenReturn(List.of(mine));

        experienceService.reorder(websiteId, caller, List.of(UUID.randomUUID(), mine.getId()));

        assertThat(mine.getSortOrder()).isZero();
    }

    @Test
    void listingOnlyNeedsReadAccess() {
        when(repository.findByWebsiteIdOrderBySortOrder(websiteId)).thenReturn(List.of());

        experienceService.list(websiteId, caller);

        verify(accessGuard).requireReadAccess(websiteId, caller);
        verify(accessGuard, never()).requirePermission(any(), any(), any());
    }
}
