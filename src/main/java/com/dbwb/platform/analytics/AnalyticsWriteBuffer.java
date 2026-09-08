package com.dbwb.platform.analytics;

import com.dbwb.platform.analytics.entity.AnalyticsEvent;
import com.dbwb.platform.analytics.repository.AnalyticsEventRepository;
import com.dbwb.platform.common.config.AnalyticsWriteProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Visits are counted in memory and written to the table in batches.
 *
 * Every page view used to be its own INSERT in its own transaction. Measured
 * on a published shop under fifty concurrent readers, that was 35,000 rows in
 * fifteen seconds - each one taking a connection out of the same pool the
 * pages are served from. It was already off the request thread, which helped
 * with latency and not at all with the pool.
 *
 * Worse, the executor behind it used CallerRunsPolicy: once its queue filled,
 * the visitor's own request thread did the INSERT. That is a defensible policy
 * for work that matters and the wrong one for this - it means a rush of
 * traffic makes the site slower for the very people causing it, to keep a
 * number that nobody reads until tomorrow.
 *
 * So: recording a visit is now an enqueue and nothing else - no transaction,
 * no connection, no thread handed off. A scheduled flush empties the queue in
 * batches. The rows written are exactly the rows that were written before, so
 * every query, export and dashboard reading them is untouched; only when they
 * arrive has changed.
 *
 * Two things are given up on purpose, both already the stated position of this
 * path: a crash loses the last couple of seconds of counts, and a flood past
 * the buffer's ceiling is counted as dropped rather than growing the heap. A
 * visit count is a trend, not a ledger.
 */
@Component
public class AnalyticsWriteBuffer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsWriteBuffer.class);

    private final AnalyticsEventRepository repository;
    private final AnalyticsWriteProperties properties;

    private final Queue<AnalyticsEvent> pending = new ConcurrentLinkedQueue<>();
    /** ConcurrentLinkedQueue.size() walks the whole queue, so the count is kept alongside it. */
    private final AtomicInteger pendingCount = new AtomicInteger();
    private final AtomicLong dropped = new AtomicLong();

    public AnalyticsWriteBuffer(AnalyticsEventRepository repository, AnalyticsWriteProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /** Returns false when the event was dropped because the buffer is full. */
    public boolean record(AnalyticsEvent event) {
        if (pendingCount.get() >= properties.getBufferCapacity()) {
            dropped.incrementAndGet();
            return false;
        }
        pending.add(event);
        pendingCount.incrementAndGet();
        return true;
    }

    @Scheduled(fixedDelayString = "${dbwb.analytics.flush-interval-ms}")
    public void flush() {
        int written = 0;
        // A batch at a time, so one very busy interval cannot build a single
        // enormous transaction, and so writes that fail take the batch down
        // with them rather than everything that had accumulated.
        for (List<AnalyticsEvent> batch = drain(); !batch.isEmpty(); batch = drain()) {
            try {
                writeBatch(batch);
                written += batch.size();
            } catch (RuntimeException e) {
                // Never propagate: this runs on the scheduler, and a throw here
                // would stop the flush from being scheduled again - turning a
                // transient database error into analytics that never resume.
                log.warn("Dropped {} analytics rows that could not be written.", batch.size(), e);
            }
        }

        long lost = dropped.getAndSet(0);
        if (lost > 0) {
            // Loud on purpose: if this appears the platform is taking more
            // traffic than the buffer was sized for, which is worth knowing.
            log.warn("Analytics buffer was full: {} visits not counted. Wrote {} rows.", lost, written);
        }
    }

    /** Visible for the shutdown hook and for tests, which must not wait two seconds. */
    @Transactional
    public void writeBatch(List<AnalyticsEvent> batch) {
        repository.saveAll(batch);
    }

    private List<AnalyticsEvent> drain() {
        List<AnalyticsEvent> batch = new ArrayList<>();
        for (int i = 0; i < properties.getBatchSize(); i++) {
            AnalyticsEvent event = pending.poll();
            if (event == null) break;
            pendingCount.decrementAndGet();
            batch.add(event);
        }
        return batch;
    }

    /** How many visits are waiting to be written. For tests and for a health check. */
    public int pending() {
        return pendingCount.get();
    }
}
