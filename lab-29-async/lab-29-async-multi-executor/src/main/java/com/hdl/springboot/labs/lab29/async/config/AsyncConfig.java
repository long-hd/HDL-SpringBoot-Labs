package com.hdl.springboot.labs.lab29.async.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync // Bật hỗ trợ cho @Async
public class AsyncConfig {

    public static final String EXECUTOR_ONE = "executorOne";
    public static final String EXECUTOR_TWO = "executorTwo";

    @Bean(name = EXECUTOR_ONE)
    public ThreadPoolTaskExecutor executorOne() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("task-one-");
        executor.initialize();
        return executor;
    }

    @Bean(name = EXECUTOR_TWO)
    public ThreadPoolTaskExecutor executorTwo() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("task-two-");
        executor.initialize();
        return executor;
    }

}
