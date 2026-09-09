package com.dbwb.platform.events;

import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.events.dto.EventDetailsRequest;
import com.dbwb.platform.events.dto.ScheduleEntryRequest;
import com.dbwb.platform.events.entity.EventDetails;
import com.dbwb.platform.events.entity.EventScheduleEntry;
import com.dbwb.platform.events.repository.EventDetailsRepository;
import com.dbwb.platform.events.repository.EventScheduleEntryRepository;
import com.dbwb.platform.manager.entity.Permission;
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

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventDetailsRepository detailsRepository;
    @Mock private EventScheduleEntryRepository scheduleRepository;
    @Mock private WebsiteAccessGuard accessGuard;

    private EventService eventService;

    private final UUID websiteId = UUID.randomUUID();
    private final AuthenticatedAccount caller =
            new AuthenticatedAccount(UUID.randomUUID(), "host@example.com", Role.BUSINESS_OWNER);
    private BusinessWebsite website;

    @BeforeEach
    void setUp() {
        eventService = new EventService(detailsRepository, scheduleRepository, accessGuard);
        website = TestEntities.withId(new BusinessWebsite(), websiteId);
        lenient().when(accessGuard.requirePermission(eq(websiteId), eq(caller), any())).thenReturn(website);
    }

    @Test
    void createsTheDetailsRowOnFirstSave() {
        // A website that switched to EVENTS later has no row. The editor should
        // just work rather than the owner tripping over a missing one.
        when(detailsRepository.findByWebsiteId(websiteId)).thenReturn(Optional.empty());
        when(detailsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventDetails saved = eventService.saveDetails(websiteId, caller, new EventDetailsRequest(
                "14 June 2026", "6:00 PM", null, "The Old Orangery", "Black tie", "by the end of May", "Parking on site."));

        assertThat(saved.getWebsite()).isSameAs(website);
        assertThat(saved.getEventDate()).isEqualTo("14 June 2026");
        assertThat(saved.getVenueName()).isEqualTo("The Old Orangery");
        assertThat(saved.getEndTime()).isNull();
    }

    @Test
    void updatesTheExistingRowRatherThanAddingASecond() {
        EventDetails existing = new EventDetails();
        existing.setWebsite(website);
        existing.setEventDate("old date");
        when(detailsRepository.findByWebsiteId(websiteId)).thenReturn(Optional.of(existing));
        when(detailsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        eventService.saveDetails(websiteId, caller,
                new EventDetailsRequest("new date", null, null, null, null, null, null));

        assertThat(existing.getEventDate()).isEqualTo("new date");
        // Clearing a field must clear it, not leave the old value behind.
        assertThat(existing.getVenueName()).isNull();
    }

    @Test
    void savingIsGatedOnManagingContent() {
        when(detailsRepository.findByWebsiteId(websiteId)).thenReturn(Optional.empty());
        when(detailsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        eventService.saveDetails(websiteId, caller, new EventDetailsRequest(null, null, null, null, null, null, null));

        verify(accessGuard).requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
    }

    @Test
    void addsScheduleEntriesToTheEndOfTheHostsOrder() {
        when(scheduleRepository.findByWebsiteIdOrderBySortOrder(websiteId))
                .thenReturn(List.of(entry("Ceremony", 0), entry("Dinner", 1)));
        when(scheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventScheduleEntry added = eventService.addScheduleEntry(
                websiteId, caller, new ScheduleEntryRequest("10:00 PM", "Dancing", null));

        assertThat(added.getSortOrder()).isEqualTo(2);
        assertThat(added.getTitle()).isEqualTo("Dancing");
    }

    @Test
    void refusesAScheduleEntryBelongingToAnotherWebsite() {
        UUID entryId = UUID.randomUUID();
        EventScheduleEntry foreign = new EventScheduleEntry();
        foreign.setWebsite(TestEntities.withId(new BusinessWebsite(), UUID.randomUUID()));
        when(scheduleRepository.findById(entryId)).thenReturn(Optional.of(TestEntities.withId(foreign, entryId)));

        assertThatThrownBy(() -> eventService.updateScheduleEntry(
                websiteId, entryId, caller, new ScheduleEntryRequest(null, "Hijacked", null)))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(foreign.getTitle()).isNull();
    }

    @Test
    void refusesToDeleteAScheduleEntryBelongingToAnotherWebsite() {
        UUID entryId = UUID.randomUUID();
        EventScheduleEntry foreign = new EventScheduleEntry();
        foreign.setWebsite(TestEntities.withId(new BusinessWebsite(), UUID.randomUUID()));
        when(scheduleRepository.findById(entryId)).thenReturn(Optional.of(TestEntities.withId(foreign, entryId)));

        assertThatThrownBy(() -> eventService.deleteScheduleEntry(websiteId, entryId, caller))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(scheduleRepository, never()).delete(any());
    }

    @Test
    void reorderIgnoresIdsFromAnotherWebsite() {
        EventScheduleEntry first = entry("Ceremony", 0);
        EventScheduleEntry second = entry("Dinner", 1);
        when(scheduleRepository.findByWebsiteIdOrderBySortOrder(websiteId)).thenReturn(List.of(first, second));

        // A stale tab submitting someone else's id must not shift this site's rows.
        eventService.reorderSchedule(websiteId, caller, List.of(UUID.randomUUID(), second.getId(), first.getId()));

        assertThat(second.getSortOrder()).isZero();
        assertThat(first.getSortOrder()).isEqualTo(1);
    }

    private EventScheduleEntry entry(String title, int sortOrder) {
        EventScheduleEntry entry = new EventScheduleEntry();
        entry.setWebsite(website);
        entry.setTitle(title);
        entry.setSortOrder(sortOrder);
        return TestEntities.withId(entry, UUID.randomUUID());
    }
}
