package com.dbwb.platform.analytics;

import com.dbwb.platform.analytics.repository.AnalyticsEventRepository;
import com.dbwb.platform.common.config.BusinessRuleProperties;
import com.dbwb.platform.menu.repository.MenuItemRepository;
import com.dbwb.platform.subscription.SubscriptionQueryService;
import com.dbwb.platform.website.WebsiteAccessGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * analytics_events had no retention. A row is written on every public page
 * load and every item view, on a path with no other bound, so the table grew
 * forever.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsRetentionTest {

    @Mock private AnalyticsEventRepository repository;
    @Mock private MenuItemRepository menuItemRepository;
    @Mock private WebsiteAccessGuard accessGuard;
    @Mock private SubscriptionQueryService subscriptionQueryService;

    private AnalyticsService serviceWithRetention(int days) {
        BusinessRuleProperties rules = new BusinessRuleProperties();
        rules.setAnalyticsEventRetentionDays(days);
        return new AnalyticsService(repository, menuItemRepository, accessGuard, subscriptionQueryService, rules);
    }

    @Test
    void discardsEventsOlderThanTheRetentionWindow() {
        when(repository.deleteOlderThan(org.mockito.ArgumentMatchers.any())).thenReturn(42);

        int purged = serviceWithRetention(365).purgeExpiredEvents();

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(repository).deleteOlderThan(cutoff.capture());
        assertThat(cutoff.getValue()).isCloseTo(
                Instant.now().minus(365, ChronoUnit.DAYS), within(1, ChronoUnit.MINUTES));
        assertThat(purged).isEqualTo(42);
    }

    @Test
    void anUnsetRetentionWindowDisablesThePurgeRatherThanDeletingEverything() {
        // A property missing from a yml binds to 0. Read literally that is a
        // cutoff of "now", which would wipe every customer's history on the
        // first maintenance pass - the same class of bug as the zero-day trial.
        assertThat(serviceWithRetention(0).purgeExpiredEvents()).isZero();
        assertThat(serviceWithRetention(-5).purgeExpiredEvents()).isZero();

        verify(repository, never()).deleteOlderThan(org.mockito.ArgumentMatchers.any());
    }
}
