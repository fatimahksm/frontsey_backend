package com.dbwb.platform.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Caching for the assembled public page.
 *
 * Measured before this existed: a page took about 119ms at the median and the
 * server saturated at roughly 480 requests a second, while the database
 * answered each of its queries in about 0.2ms. So almost none of the time was
 * the database - it was rebuilding the same 38KB of JSON from a dozen tables
 * for every visitor, over and over, for content that had not changed.
 *
 * Time-based expiry rather than eviction on every write, and that is the
 * important decision. The public payload is assembled from roughly a dozen
 * tables - menu items, categories, sizes, add-ons, box variants, profile,
 * hours, gallery, services, sections, projects, event details - so *any* edit
 * anywhere changes it. Hooking eviction into every one of those write paths
 * means the bug is one forgotten path away, and it presents as an owner
 * changing a price and not seeing it, which is the worst kind of bug to have:
 * intermittent, unreproducible, and about money.
 *
 * Ten seconds is short enough that no owner notices - switching to another tab
 * to look at their site takes longer - and long enough that at 480 requests a
 * second across 100 sites each page is built once per ten seconds instead of
 * several times a second.
 *
 * Publishing evicts explicitly on top of that, because it is the one action an
 * owner consciously waits on and watches for.
 *
 * Per instance, like the rate limiter. Two nodes each keep their own copy, so a
 * publish evicts one and the other serves the old page for up to ten more
 * seconds. That is the ceiling on staleness even with nothing shared, which is
 * exactly why the expiry is the primary mechanism and not a nicety on top.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** The assembled public page, keyed by slug. */
    public static final String PUBLIC_WEBSITES = "publicWebsites";

    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(PUBLIC_WEBSITES);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(10))
                // A bound, so a flood of requests for slugs that do not exist
                // cannot grow this without limit. Ten thousand pages is far
                // more than one node needs hot at once.
                .maximumSize(10_000));
        return manager;
    }
}
