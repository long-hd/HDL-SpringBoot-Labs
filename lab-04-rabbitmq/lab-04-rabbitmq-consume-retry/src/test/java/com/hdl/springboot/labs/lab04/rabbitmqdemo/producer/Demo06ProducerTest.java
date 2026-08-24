package com.hdl.springboot.labs.lab04.rabbitmqdemo.producer;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class Demo06ProducerTest {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private Demo06Producer producer;

    @Test
    public void testSyncSend() throws InterruptedException {
        int id = (int) System.currentTimeMillis() / 1000;
        producer.syncSend(id);
        logger.info("[testSyncSend][Mã gửi: [{}] Gửi thành công]", id);

        Thread.sleep(5000L);
    }

}
