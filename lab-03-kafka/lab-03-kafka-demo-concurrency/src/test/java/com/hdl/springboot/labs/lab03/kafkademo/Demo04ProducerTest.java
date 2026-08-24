package com.hdl.springboot.labs.lab03.kafkademo;

import com.hdl.springboot.labs.lab03.kafkademo.producer.Demo04Producer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.ExecutionException;

@SpringBootTest
public class Demo04ProducerTest {

    Logger logger = LoggerFactory.getLogger(Demo04ProducerTest.class);

    @Autowired
    private Demo04Producer producer;

    @Test
    public void testSyncSend() throws ExecutionException, InterruptedException {
        for(int i = 0; i < 10; i++) {
            int id = (int) System.currentTimeMillis() / 1000;
            producer.syncSend(id);
        }

        Thread.sleep(50_000);
    }

    @Test
    public void testSyncSendOrderly() throws ExecutionException, InterruptedException {
        for(int i = 0; i < 10; i++) {
            int id = 1;
            SendResult result = producer.syncSendOrderly(id);
            logger.info("[testSyncSendOrderly][Số thứ tự gửi: [{}], Partition gửi: [{}]]",
                    id, result.getRecordMetadata().partition());
        }

        Thread.sleep(50_000);
    }

}
