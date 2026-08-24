package com.hdl.springboot.labs.lab03.kafkademo.producer;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

@SpringBootTest
public class Demo02ProducerTest {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private Demo02Producer producer;

    @Test
    public void testSyncSend() throws ExecutionException, InterruptedException {
        int id = (int) System.currentTimeMillis() / 1000;
        SendResult<String, Object> result = producer.syncSend(id);
        logger.info("[testSyncSend][Mã gửi: [{}] Kết quả gửi: [{}]]", id, result);


        Thread.sleep(100_000L);
    }

    @Test
    public void testSyncSendLoop() throws ExecutionException, InterruptedException {
        for (int i = 0; i < 100; i++) {
            int id = (int) System.currentTimeMillis() / 1000;
            SendResult<String, Object> result = producer.syncSend(id);
            logger.info("[testSyncSend][Mã gửi: [{}] Kết quả gửi: [{}]]", id, result);
        }

        new CountDownLatch(1).await();
    }

}
