package com.dbwb.platform.analytics;

import com.dbwb.platform.analytics.entity.AnalyticsEvent;
import com.dbwb.platform.analytics.entity.AnalyticsEventType;
import com.dbwb.platform.analytics.entity.DeviceType;
import com.dbwb.platform.analytics.repository.AnalyticsEventRepository;
import com.dbwb.platform.common.config.AnalyticsWriteProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The point of the buffer is what a visitor's request does NOT do, so that is
 * what these assert: no database, at all, until the flush.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsWriteBufferTest {

    @Mock private AnalyticsEventRepository repository;

    private AnalyticsWriteProperties properties;
    private AnalyticsWriteBuffer buffer;

    @BeforeEach
    void setUp() {
        properties = new AnalyticsWriteProperties();
        properties.setBatchSize(500);
        properties.setBufferCapacity(20000);
        buffer = new AnalyticsWriteBuffer(repository, properties);
    }

    private static AnalyticsEvent pageView() {
        AnalyticsEvent event = new AnalyticsEvent();
        event.setWebsiteId(UUID.randomUUID());
        event.setEventType(AnalyticsEventType.PAGE_VIEW);
        event.setDeviceType(DeviceType.MOBILE);
        return event;
    }

    @Test
    void recordingAVisitDoesNotTouchTheDatabase() {
        for (int i = 0; i < 100; i++) {
            buffer.record(pageView());
        }

        // The whole change, in one assertion: a hundred visitors, no
        // connections taken out of the pool the pages are served from.
        verifyNoInteractions(repository);
        assertThat(buffer.pending()).isEqualTo(100);
    }

    @Test
    void theFlushWritesEverythingThatWasWaitingInOneBatch() {
        for (int i = 0; i < 100; i++) {
            buffer.record(pageView());
        }

        buffer.flush();

        ArgumentCaptor<Iterable<AnalyticsEvent>> written = ArgumentCaptor.captor();
        verify(repository).saveAll(written.capture());
        assertThat(written.getValue()).hasSize(100);
        assertThat(buffer.pending()).isZero();
    }

    @Test
    void moreThanOneBatchIsWrittenAsSeveral() {
        properties.setBatchSize(10);
        for (int i = 0; i < 25; i++) {
            buffer.record(pageView());
        }

        buffer.flush();

        // 10 + 10 + 5, not one transaction holding twenty-five.
        verify(repository, times(3)).saveAll(anyIterable());
        assertThat(buffer.pending()).isZero();
    }

    @Test
    void aFullBufferDropsVisitsRatherThanGrowing() {
        properties.setBufferCapacity(10);

        List<Boolean> accepted = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            accepted.add(buffer.record(pageView()));
        }

        // The ceiling is the point: a flood is counted as lost, not held.
        assertThat(buffer.pending()).isEqualTo(10);
        assertThat(accepted.stream().filter(Boolean::booleanValue).count()).isEqualTo(10);
    }

    @Test
    void aFailingWriteDoesNotStopTheNextFlush() {
        org.mockito.Mockito.when(repository.saveAll(anyIterable()))
                .thenThrow(new RuntimeException("database went away"));
        buffer.record(pageView());

        // Must not propagate: this runs on the scheduler, and a throw would
        // cancel the schedule and stop analytics for the life of the process.
        buffer.flush();

        assertThat(buffer.pending()).isZero();
    }

    @Test
    void countsFromManyThreadsAtOnceAreNotLost() throws Exception {
        int threads = 16;
        int perThread = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) buffer.record(pageView());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(20, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        // Every concurrent visitor is one row, and the count kept alongside the
        // queue agrees with the queue - the two drifting apart would leak the
        // buffer's capacity a request at a time.
        assertThat(buffer.pending()).isEqualTo(threads * perThread);
    }
}
