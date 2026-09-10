package com.visionselect.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configures Spring asynchronous execution and scheduling for the AI job pipeline.
 *
 * <p>{@code @EnableAsync} activates {@code @Async}-annotated methods; every async
 * invocation runs on the {@code aiJobExecutor} thread pool rather than the caller's
 * thread or an unmanaged {@code CompletableFuture} pool.
 *
 * <p>{@code @EnableScheduling} activates {@code @Scheduled} polling methods used by
 * the retry dispatcher and stalled-job detector in {@code AiJobService}.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * Dedicated thread pool for AI job dispatch.
     *
     * <p>Core pool: 4 threads kept alive when idle.
     * Max pool: 10 threads under burst load.
     * Queue capacity: 50 — backpressure before creating more threads.
     * Thread naming: "ai-job-executor-N" for easy log filtering.
     * Await termination: 30 s graceful shutdown so in-flight jobs complete.
     */
    @Bean(name = "aiJobExecutor")
    public Executor aiJobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-job-executor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
