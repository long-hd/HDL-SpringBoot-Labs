package com.hdl.springboot.labs.lab29.async.config;

import com.hdl.springboot.labs.lab29.async.core.GlobalAsyncExceptionHandler;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // Bật hỗ trợ cho @Async
public class AsyncConfig implements AsyncConfigurer {

    private final Executor taskExecutor;
    private final GlobalAsyncExceptionHandler globalAsyncExceptionHandler;

    public AsyncConfig(
            @Qualifier("applicationTaskExecutor") Executor taskExecutor,
            GlobalAsyncExceptionHandler globalAsyncExceptionHandler
    ) {
        this.taskExecutor = taskExecutor;
        this.globalAsyncExceptionHandler = globalAsyncExceptionHandler;
    }

    public Executor getAsyncExecutor() {
        return taskExecutor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return globalAsyncExceptionHandler;
    }

}
