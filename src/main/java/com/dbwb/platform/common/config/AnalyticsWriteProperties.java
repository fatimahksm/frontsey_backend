package com.dbwb.platform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * How visits get from a request to the analytics table (dbwb.analytics.*).
 *
 * Like every other business number here these live in configuration, so a
 * deployment that would rather lose fewer counts can flush more often without
 * a code change.
 */
@Configuration
@ConfigurationProperties(prefix = "dbwb.analytics")
public class AnalyticsWriteProperties {

    /**
     * How many unwritten visits may be held in memory.
     *
     * The ceiling is the whole point: past it, counting stops rather than the
     * process growing until it falls over. Sized so a busy minute fits - at
     * roughly 200 bytes a row this is a few megabytes at worst.
     */
    private int bufferCapacity = 20000;

    /** How often the buffer is emptied into the table. */
    private long flushIntervalMs = 2000;

    /** Rows per insert batch. Matches hibernate.jdbc.batch_size. */
    private int batchSize = 500;

    public int getBufferCapacity() {
        return bufferCapacity;
    }

    public void setBufferCapacity(int bufferCapacity) {
        this.bufferCapacity = bufferCapacity;
    }

    public long getFlushIntervalMs() {
        return flushIntervalMs;
    }

    public void setFlushIntervalMs(long flushIntervalMs) {
        this.flushIntervalMs = flushIntervalMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
