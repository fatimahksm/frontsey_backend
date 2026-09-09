package com.dbwb.platform.events.repository;

import com.dbwb.platform.events.entity.EventScheduleEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventScheduleEntryRepository extends JpaRepository<EventScheduleEntry, UUID> {
    List<EventScheduleEntry> findByWebsiteIdOrderBySortOrder(UUID websiteId);
}
