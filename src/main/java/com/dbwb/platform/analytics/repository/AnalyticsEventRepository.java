package com.dbwb.platform.analytics.repository;

import com.dbwb.platform.analytics.entity.AnalyticsEvent;
import com.dbwb.platform.analytics.entity.AnalyticsEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {

    long countByWebsiteIdAndEventTypeAndCreatedAtBetween(
            UUID websiteId, AnalyticsEventType eventType, Instant from, Instant to);

    @Query("""
            select cast(e.itemId as string) as key, count(e) as total from AnalyticsEvent e
            where e.websiteId = :websiteId and e.eventType = 'ITEM_VIEW' and e.itemId is not null
              and e.createdAt between :from and :to
            group by e.itemId order by count(e) desc
            """)
    List<KeyCount> mostViewedItems(@Param("websiteId") UUID websiteId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select coalesce(e.referralSource, 'direct') as key, count(e) as total from AnalyticsEvent e
            where e.websiteId = :websiteId and e.eventType = 'PAGE_VIEW'
              and e.createdAt between :from and :to
            group by e.referralSource order by count(e) desc
            """)
    List<KeyCount> visitsByReferralSource(@Param("websiteId") UUID websiteId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select cast(e.deviceType as string) as key, count(e) as total from AnalyticsEvent e
            where e.websiteId = :websiteId and e.eventType = 'PAGE_VIEW'
              and e.createdAt between :from and :to
            group by e.deviceType order by count(e) desc
            """)
    List<KeyCount> visitsByDeviceType(@Param("websiteId") UUID websiteId, @Param("from") Instant from, @Param("to") Instant to);

    /** Generic key -> count projection shared by every grouped analytics query above. */
    interface KeyCount {
        String getKey();

        long getTotal();
    }
}
