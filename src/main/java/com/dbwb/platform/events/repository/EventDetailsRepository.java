package com.dbwb.platform.events.repository;

import com.dbwb.platform.events.entity.EventDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventDetailsRepository extends JpaRepository<EventDetails, UUID> {
    Optional<EventDetails> findByWebsiteId(UUID websiteId);
}
