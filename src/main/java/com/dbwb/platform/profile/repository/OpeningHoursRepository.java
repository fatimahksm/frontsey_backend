package com.dbwb.platform.profile.repository;

import com.dbwb.platform.profile.entity.OpeningHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OpeningHoursRepository extends JpaRepository<OpeningHours, UUID> {
    List<OpeningHours> findByWebsiteIdOrderByDayOfWeek(UUID websiteId);
}
