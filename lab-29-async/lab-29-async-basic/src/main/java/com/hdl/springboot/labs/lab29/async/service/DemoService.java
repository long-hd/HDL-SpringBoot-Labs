package com.hdl.springboot.labs.lab29.async.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class DemoService {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Async
    public void execute01Async() {
        this.execute01();
    }

    @Async
    public void execute02Async() {
        this.execute02();
    }

    @Async
    public CompletableFuture<Integer> execute01AsyncWithFuture() {
        return CompletableFuture.completedFuture(this.execute01());
    }

    @Async
    public CompletableFuture<Integer> execute02AsyncWithFuture() {
        return CompletableFuture.completedFuture(this.execute02());
    }

    @Async
    public CompletableFuture<Integer> execute01AsyncWithCompletableFuture() {
        try {
            return CompletableFuture.completedFuture(this.execute01());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public Integer execute01() {
        logger.info("[execute01]");
        sleep(10);
        return 1;
    }

    public Integer execute02() {
        logger.info("[execute02]");
        sleep(5);
        return 2;
    }

    public static void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        }catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /** Exception async — void để AsyncUncaughtExceptionHandler bắt chắc hơn */
    @Async
    public void boom(Integer a, Integer b) {
        throw new RuntimeException("async failed: a=" + a + ", b=" + b);
    }

}
