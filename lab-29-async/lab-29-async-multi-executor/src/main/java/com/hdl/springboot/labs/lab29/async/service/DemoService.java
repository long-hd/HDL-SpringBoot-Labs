package com.hdl.springboot.labs.lab29.async.service;

import com.hdl.springboot.labs.lab29.async.config.AsyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class DemoService {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Async(AsyncConfig.EXECUTOR_ONE)
    public void execute01() {
        logger.info("[execute01] thread={}", Thread.currentThread().getName());
    }

    @Async(AsyncConfig.EXECUTOR_TWO)
    public CompletableFuture<Integer> execute02() {
        logger.info("[execute02] thread={}", Thread.currentThread().getName());
        return CompletableFuture.completedFuture(2);
    }

}
