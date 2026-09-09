package com.dbwb.platform.events;

import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.events.dto.EventDetailsRequest;
import com.dbwb.platform.events.dto.ScheduleEntryRequest;
import com.dbwb.platform.events.entity.EventDetails;
import com.dbwb.platform.events.entity.EventScheduleEntry;
import com.dbwb.platform.events.repository.EventDetailsRepository;
import com.dbwb.platform.events.repository.EventScheduleEntryRepository;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.website.WebsiteAccessGuard;
import com.dbwb.platform.website.entity.BusinessWebsite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The occasion and its running order, on an EVENTS website. Gated on
 * MANAGE_THEME_AND_CONTENT, the same permission as the gallery and custom
 * sections - this is website content, not a catalogue.
 */
@Service
public class EventService {

    private final EventDetailsRepository detailsRepository;
    private final EventScheduleEntryRepository scheduleRepository;
    private final WebsiteAccessGuard accessGuard;

    public EventService(EventDetailsRepository detailsRepository,
                        EventScheduleEntryRepository scheduleRepository,
                        WebsiteAccessGuard accessGuard) {
        this.detailsRepository = detailsRepository;
        this.scheduleRepository = scheduleRepository;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public Optional<EventDetails> getDetails(UUID websiteId, AuthenticatedAccount caller) {
        accessGuard.requireReadAccess(websiteId, caller);
        return detailsRepository.findByWebsiteId(websiteId);
    }

    /**
     * Creates the row on first save rather than at website creation. A website
     * that switched to EVENTS later has no row, and the editor should just work
     * rather than the owner having to trip over a missing one.
     */
    @Transactional
    public EventDetails saveDetails(UUID websiteId, AuthenticatedAccount caller, EventDetailsRequest request) {
        BusinessWebsite website = accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        EventDetails details = detailsRepository.findByWebsiteId(websiteId).orElseGet(() -> {
            EventDetails fresh = new EventDetails();
            fresh.setWebsite(website);
            return fresh;
        });
        details.setEventDate(request.eventDate());
        details.setStartTime(request.startTime());
        details.setEndTime(request.endTime());
        details.setVenueName(request.venueName());
        details.setDressCode(request.dressCode());
        details.setRsvpBy(request.rsvpBy());
        details.setNote(request.note());
        return detailsRepository.save(details);
    }

    @Transactional(readOnly = true)
    public List<EventScheduleEntry> listSchedule(UUID websiteId, AuthenticatedAccount caller) {
        accessGuard.requireReadAccess(websiteId, caller);
        return scheduleRepository.findByWebsiteIdOrderBySortOrder(websiteId);
    }

    @Transactional
    public EventScheduleEntry addScheduleEntry(UUID websiteId, AuthenticatedAccount caller, ScheduleEntryRequest request) {
        BusinessWebsite website = accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        EventScheduleEntry entry = new EventScheduleEntry();
        entry.setWebsite(website);
        apply(entry, request);
        // Appended, not inserted: a new line goes to the end of the host's
        // existing order rather than silently reshuffling it.
        entry.setSortOrder(scheduleRepository.findByWebsiteIdOrderBySortOrder(websiteId).size());
        return scheduleRepository.save(entry);
    }

    @Transactional
    public EventScheduleEntry updateScheduleEntry(
            UUID websiteId, UUID entryId, AuthenticatedAccount caller, ScheduleEntryRequest request) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        EventScheduleEntry entry = requireOwned(websiteId, entryId);
        apply(entry, request);
        return entry;
    }

    @Transactional
    public void deleteScheduleEntry(UUID websiteId, UUID entryId, AuthenticatedAccount caller) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        scheduleRepository.delete(requireOwned(websiteId, entryId));
    }

    /**
     * Rewrites the running order from the given id sequence. Ids that do not
     * belong to this website are ignored rather than rejected, so a stale tab
     * reordering a list cannot move another site's rows.
     */
    @Transactional
    public List<EventScheduleEntry> reorderSchedule(UUID websiteId, AuthenticatedAccount caller, List<UUID> orderedIds) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_THEME_AND_CONTENT);
        List<EventScheduleEntry> entries = scheduleRepository.findByWebsiteIdOrderBySortOrder(websiteId);
        int position = 0;
        for (UUID id : orderedIds) {
            for (EventScheduleEntry entry : entries) {
                if (entry.getId().equals(id)) {
                    entry.setSortOrder(position++);
                    break;
                }
            }
        }
        return scheduleRepository.findByWebsiteIdOrderBySortOrder(websiteId);
    }

    /**
     * An entry id alone is not enough: without checking it belongs to this
     * website, a caller with rights to their own site could edit anyone's
     * schedule by guessing an id.
     */
    private EventScheduleEntry requireOwned(UUID websiteId, UUID entryId) {
        EventScheduleEntry entry = scheduleRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule entry not found."));
        if (!entry.getWebsite().getId().equals(websiteId)) {
            throw new ResourceNotFoundException("Schedule entry not found.");
        }
        return entry;
    }

    private void apply(EventScheduleEntry entry, ScheduleEntryRequest request) {
        entry.setTime(request.time());
        entry.setTitle(request.title());
        entry.setDetail(request.detail());
    }
}
