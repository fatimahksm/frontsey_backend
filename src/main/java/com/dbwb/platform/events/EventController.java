package com.dbwb.platform.events;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.events.dto.EventDetailsRequest;
import com.dbwb.platform.events.dto.EventDetailsResponse;
import com.dbwb.platform.events.dto.ScheduleEntryRequest;
import com.dbwb.platform.events.dto.ScheduleEntryResponse;
import com.dbwb.platform.security.CurrentAccount;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/websites/{websiteId}/event")
public class EventController {

    private final EventService eventService;
    private final CurrentAccount currentAccount;

    public EventController(EventService eventService, CurrentAccount currentAccount) {
        this.eventService = eventService;
        this.currentAccount = currentAccount;
    }

    /** Empty rather than 404 when nothing has been filled in yet, so the editor always has a shape to bind to. */
    @GetMapping
    public ApiResponse<EventDetailsResponse> details(@PathVariable UUID websiteId) {
        return ApiResponse.ok(eventService.getDetails(websiteId, currentAccount.get())
                .map(EventDetailsResponse::from)
                .orElseGet(EventDetailsResponse::empty));
    }

    @PutMapping
    public ApiResponse<EventDetailsResponse> saveDetails(
            @PathVariable UUID websiteId, @RequestBody EventDetailsRequest request) {
        return ApiResponse.ok(EventDetailsResponse.from(
                eventService.saveDetails(websiteId, currentAccount.get(), request)));
    }

    @GetMapping("/schedule")
    public ApiResponse<List<ScheduleEntryResponse>> schedule(@PathVariable UUID websiteId) {
        return ApiResponse.ok(eventService.listSchedule(websiteId, currentAccount.get())
                .stream().map(ScheduleEntryResponse::from).toList());
    }

    @PostMapping("/schedule")
    public ApiResponse<ScheduleEntryResponse> addEntry(
            @PathVariable UUID websiteId, @Valid @RequestBody ScheduleEntryRequest request) {
        return ApiResponse.ok(ScheduleEntryResponse.from(
                eventService.addScheduleEntry(websiteId, currentAccount.get(), request)));
    }

    @PutMapping("/schedule/{entryId}")
    public ApiResponse<ScheduleEntryResponse> updateEntry(
            @PathVariable UUID websiteId, @PathVariable UUID entryId, @Valid @RequestBody ScheduleEntryRequest request) {
        return ApiResponse.ok(ScheduleEntryResponse.from(
                eventService.updateScheduleEntry(websiteId, entryId, currentAccount.get(), request)));
    }

    @DeleteMapping("/schedule/{entryId}")
    public ApiResponse<Void> deleteEntry(@PathVariable UUID websiteId, @PathVariable UUID entryId) {
        eventService.deleteScheduleEntry(websiteId, entryId, currentAccount.get());
        return ApiResponse.ok(null);
    }

    @PutMapping("/schedule/reorder")
    public ApiResponse<List<ScheduleEntryResponse>> reorder(
            @PathVariable UUID websiteId, @RequestBody List<UUID> orderedIds) {
        return ApiResponse.ok(eventService.reorderSchedule(websiteId, currentAccount.get(), orderedIds)
                .stream().map(ScheduleEntryResponse::from).toList());
    }
}
