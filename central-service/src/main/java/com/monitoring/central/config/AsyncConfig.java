package com.monitoring.central.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configures the thread pool used for @Async alarm evaluation.
 *
 * Why a dedicated thread pool?
 * - The web request thread returns immediately to the caller (warehouse service).
 * - Alarm evaluation and logging happen asynchronously, preventing back-pressure
 *   on the REST layer if alarm processing is slow (e.g., sending emails/SMS later).
 * - Core/max pool sizing: sensors send at low frequency; 2-4 threads is sufficient.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "alarmExecutor")
    public Executor alarmExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("alarm-worker-");
        executor.initialize();
        return executor;
    }
}
