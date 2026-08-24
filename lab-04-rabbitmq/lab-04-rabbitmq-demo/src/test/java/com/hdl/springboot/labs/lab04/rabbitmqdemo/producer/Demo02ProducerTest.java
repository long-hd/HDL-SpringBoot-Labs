package com.hdl.springboot.labs.lab04.rabbitmqdemo.producer;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class Demo02ProducerTest {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private Demo02Producer producer;

    @Test
    public void testSyncSendSuccess() throws InterruptedException {
        int id = (int) System.currentTimeMillis() / 1000;
        producer.syncSend(id, "sp.hdl.mq");

        logger.info("[testAsyncSend][ID gửi: [{}] Gọi xong]", id);
        Thread.sleep(3000L);
    }

    @Test
    public void testSyncSendFailure() throws InterruptedException {
        int id = (int) System.currentTimeMillis() / 1000;
        producer.syncSend(id, "abc.hdl.mq123");

        logger.info("[testAsyncSend][ID gửi: [{}] Gọi xong]", id);
        Thread.sleep(3000L);
    }

}
