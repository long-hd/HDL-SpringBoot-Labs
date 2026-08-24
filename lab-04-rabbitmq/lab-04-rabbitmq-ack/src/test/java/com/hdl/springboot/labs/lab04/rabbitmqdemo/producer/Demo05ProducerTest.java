package com.hdl.springboot.labs.lab04.rabbitmqdemo.producer;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class Demo05ProducerTest {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private Demo05Producer producer;

    @Test
    public void testSend() throws InterruptedException {
        for (int i = 1; i <= 2; i++) {
            producer.syncSend(i);
            logger.info("[testAsyncSend][ID gửi: [{}] Gọi xong]", i);
        }

        Thread.sleep(300000L);
    }

}
