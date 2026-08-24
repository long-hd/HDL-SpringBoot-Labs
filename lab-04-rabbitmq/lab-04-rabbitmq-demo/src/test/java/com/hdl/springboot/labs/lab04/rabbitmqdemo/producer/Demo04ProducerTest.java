package com.hdl.springboot.labs.lab04.rabbitmqdemo.producer;

import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo04Message;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class Demo04ProducerTest {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private Demo04Producer producer;

    @Test
    public void testSendSuccess() throws InterruptedException {
        int id = (int) System.currentTimeMillis() / 1000;
        producer.syncSend(id, Demo04Message.HEADER_VALUE);

        logger.info("[testAsyncSend][ID gửi: [{}] Gọi xong]", id);
        Thread.sleep(3000L);
    }

    @Test
    public void testSendFail() throws InterruptedException {
        int id = (int) System.currentTimeMillis() / 1000;
        producer.syncSend(id, "error");

        logger.info("[testAsyncSend][ID gửi: [{}] Gọi xong]", id);
        Thread.sleep(3000L);
    }

}
