package com.dbwb.platform.sections;

import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.sections.dto.PageSectionRequest;
import com.dbwb.platform.sections.entity.PageSection;
import com.dbwb.platform.sections.entity.PageSectionType;
import com.dbwb.platform.sections.repository.PageSectionRepository;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageSectionServiceTest {

    @Mock
    private WebsiteAccessGuard accessGuard;
    @Mock
    private PageSectionRepository repository;

    private PageSectionService sectionService;

    private final UUID websiteId = UUID.randomUUID();
    private final AuthenticatedAccount caller = new AuthenticatedAccount(UUID.randomUUID(), "owner@example.com", Role.BUSINESS_OWNER);
    private BusinessWebsite website;

    @BeforeEach
    void setUp() {
        sectionService = new PageSectionService(accessGuard, repository);
        website = TestEntities.withId(new BusinessWebsite(), websiteId);
        lenient().when(accessGuard.requirePermission(eq(websiteId), eq(caller), any())).thenReturn(website);
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    @Test
    void createAssignsTheNextSortOrder() {
        when(repository.countByWebsiteId(websiteId)).thenReturn(2L);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var section = sectionService.create(websiteId, caller,
                new PageSectionRequest(PageSectionType.ABOUT, "{\"heading\":\"Our story\"}"));

        assertThat(section.getSortOrder()).isEqualTo(2);
        assertThat(section.getType()).isEqualTo(PageSectionType.ABOUT);
        assertThat(section.getData()).isEqualTo("{\"heading\":\"Our story\"}");
    }

    @Test
    void updateRejectsASectionBelongingToAnotherWebsite() {
        PageSection foreign = new PageSection();
        foreign.setWebsite(TestEntities.withId(new BusinessWebsite(), UUID.randomUUID()));
        UUID sectionId = UUID.randomUUID();
        when(repository.findById(sectionId)).thenReturn(Optional.of(TestEntities.withId(foreign, sectionId)));

        assertThatThrownBy(() -> sectionService.update(websiteId, sectionId, caller,
                new PageSectionRequest(PageSectionType.FAQ, "{}")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reorderAppliesTheGivenOrderToEachSection() {
        PageSection first = sectionWithId();
        PageSection second = sectionWithId();
        when(repository.findById(first.getId())).thenReturn(Optional.of(first));
        when(repository.findById(second.getId())).thenReturn(Optional.of(second));

        sectionService.reorder(websiteId, caller, List.of(second.getId(), first.getId()));

        assertThat(second.getSortOrder()).isZero();
        assertThat(first.getSortOrder()).isEqualTo(1);
    }

    private PageSection sectionWithId() {
        PageSection section = new PageSection();
        section.setWebsite(website);
        return TestEntities.withId(section, UUID.randomUUID());
    }
}
