package com.dbwb.platform.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * The pool behind @Async and, through @EnableAsync's absence of alternatives,
 * the one any future background work will land in.
 *
 * Nothing uses @Async today. It existed for recording analytics off the
 * request thread, and that path now buffers in memory and writes in batches
 * instead - see AnalyticsWriteBuffer, and the measurement that prompted it.
 * Being off the request thread was never the expensive part; one transaction
 * and one connection per visitor was.
 *
 * The bean is kept rather than deleted because deleting it is the trap: with
 * @EnableAsync and no executor bean, the next @Async anyone writes silently
 * falls back to SimpleAsyncTaskExecutor, which starts a brand new thread per
 * call. A bounded pool that nothing currently uses costs two idle threads;
 * discovering that fallback under load costs a great deal more.
 *
 * CallerRunsPolicy on saturation means a full queue is run on the calling
 * thread - back-pressure rather than a silent drop. That is the right default
 * for work that matters, and was the wrong one for counting visits, which is
 * part of why that path moved.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("applicationTaskExecutor")
    public Executor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("dbwb-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // Let queued visits finish on shutdown rather than vanishing.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }
}
