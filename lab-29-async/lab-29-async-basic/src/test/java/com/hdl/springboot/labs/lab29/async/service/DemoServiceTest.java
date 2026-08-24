package com.hdl.springboot.labs.lab29.async.service;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@SpringBootTest
public class DemoServiceTest {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private DemoService demoService;

    @Test
    public void task01() {
        long now = System.currentTimeMillis();
        logger.info("[task01] start");

        demoService.execute01();
        demoService.execute02();

        logger.info("[task01] end at {}ms", System.currentTimeMillis() - now);
    }

    @Test
    public void task02() throws InterruptedException {
        long now = System.currentTimeMillis();
        logger.info("[task02] start");

        demoService.execute01Async();
        demoService.execute02Async();

        logger.info("[task02] end at {}ms", System.currentTimeMillis() - now);
    }

    @Test
    public void task03() throws ExecutionException, InterruptedException {
        long now = System.currentTimeMillis();
        logger.info("[task03] start");

        // Thực thi các tác vụ
        CompletableFuture<Integer> exec01Result = demoService.execute01AsyncWithFuture();
        CompletableFuture<Integer> exec02Result = demoService.execute02AsyncWithFuture();

        // Chặn và chờ kết quả
        exec01Result.get();
        exec02Result.get();

        logger.info("[task03] end at {}ms", System.currentTimeMillis() - now);
    }

    @Test
    public void task04() {
        long now = System.currentTimeMillis();
        logger.info("[task04] start");

        CompletableFuture<Integer> future = demoService.execute01AsyncWithCompletableFuture();
        logger.info("[task04] future type={}", future.getClass().getSimpleName());

        // thay SuccessCallback + FailureCallback
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                logger.info("[whenComplete] fail", ex);
            } else {
                logger.info("[whenComplete] result={}", result);
            }
        });

        // thay ListenableFutureCallback (gộp success/failure) — có thể chỉ cần whenComplete một lần
        future.thenAccept(result -> logger.info("[thenAccept] result={}", result))
                .exceptionally(ex -> {
                    logger.info("[exceptionally] fail", ex);
                    return null;
                });

        // chặn chờ kết quả — giống execute01Result.get()
        Integer value = future.join(); // hoặc future.get()
        logger.info("[task04] value={} end at {}ms", value, System.currentTimeMillis() - now);
    }

    @Test
    public void task06() throws InterruptedException {
        demoService.boom(1, 2);
        Thread.sleep(1000L);
    }

}
