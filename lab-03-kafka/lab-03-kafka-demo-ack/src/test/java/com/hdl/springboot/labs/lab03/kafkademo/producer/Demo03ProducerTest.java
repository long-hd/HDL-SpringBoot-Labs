package com.hdl.springboot.labs.lab03.kafkademo.producer;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.ExecutionException;

@SpringBootTest
public class Demo03ProducerTest {

    Logger logger = LoggerFactory.getLogger(Demo03ProducerTest.class);

    @Autowired
    private Demo03Producer producer;

    @Test
    public void testSyncSend() throws ExecutionException, InterruptedException {
        for (int i = 1; i <= 2; i++) {
            SendResult result = producer.syncSend(i);
            logger.info("[testSyncSend][Mã gửi: [{}] Kết quả gửi: [{}]]", i, result);
        }
        Thread.sleep(100_000L);
    }

}
