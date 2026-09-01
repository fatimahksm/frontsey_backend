package com.dbwb.platform.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * The pool behind @Async, which today means recording analytics off the request
 * thread.
 *
 * Declared rather than left to Spring's default. Without an executor bean
 * @Async falls back to SimpleAsyncTaskExecutor, which starts a brand new thread
 * for every call - on a path that fires once per page view, that is a thread
 * per visitor, which is worse than the synchronous write it replaced.
 *
 * CallerRunsPolicy on saturation is deliberate: if the queue is full the
 * request thread does the write itself. That is slow, which is the point - it
 * pushes back on the source instead of silently dropping visits, and it cannot
 * fail the response either way.
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
