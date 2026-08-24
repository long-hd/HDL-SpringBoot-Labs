package com.hdl.springboot.labs.lab04.rabbitmqdemo.producer;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class Demo01ProducerTest {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private Demo01Producer producer;

    @Test
    public void testSyncSend() throws InterruptedException {
        int id = (int) System.currentTimeMillis() / 1000;
        producer.syncSend(id);
        logger.info("[testSyncSend][ID gửi: [{}]. Gửi thành công]", id);

        // Đợi ở luồng chính để xem được log ở consumer
        Thread.sleep(3000L);
    }

    @Test
    public void testSyncSendDefault() throws InterruptedException {
        int id = (int) System.currentTimeMillis() / 1000;
        producer.syncSendDefault(id);
        logger.info("[testSyncSend][ID gửi: [{}]. Gửi mặc định thành công]", id);

        // Đợi ở luồng chính để xem được log ở consumer
        Thread.sleep(3000L);
    }

    @Test
    public void testAsyncSend() throws InterruptedException {
        int id = (int) System.currentTimeMillis() / 1000;
        producer.asyncSend(id).whenComplete((res, ex) -> {
           if (ex != null) {
               logger.info("[testAsyncSend][ID gửi: [{}]; Exception]", id, ex);
           } else {
               logger.info("[testAsyncSend][ID gửi: [{}]; Gửi thành công]", id);
           }
        });

        logger.info("[testAsyncSend][ID gửi: [{}] Gọi xong]", id);
        Thread.sleep(3000L);
    }

}
