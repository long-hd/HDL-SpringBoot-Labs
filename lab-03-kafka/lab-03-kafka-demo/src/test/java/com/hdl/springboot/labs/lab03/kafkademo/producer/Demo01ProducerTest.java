package com.hdl.springboot.labs.lab03.kafkademo.producer;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutionException;

@SpringBootTest
public class Demo01ProducerTest {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private Demo01Producer producer;

    @Test
    public void testSyncSend() throws ExecutionException, InterruptedException {
        int id = (int) System.currentTimeMillis() / 1000;
        producer.syncSend(id);
        logger.info("[testSyncSend][ID gửi: [{}]. Gửi thành công]", id);

        Thread.sleep(3000L);
    }

    @Test
    public void testAsyncSend() throws ExecutionException, InterruptedException {
        int id = (int) System.currentTimeMillis() / 1000;
        producer.asyncSend(id);
        logger.info("[testAsyncSend][ID gửi: [{}]. Gửi thành công]", id);

        Thread.sleep(3000L);
    }

}
