package com.hdl.springboot.labs.lab03.kafkademo.producer;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;

@SpringBootTest
public class Demo05ProducerTest {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private Demo05Producer producer;

    @Test
    public void testAsyncSend() throws InterruptedException {
        logger.info("[testASyncSend][Bắt đầu thực hiện]");

        for(int i=0; i < 3;  i++){
            int id = (int) System.currentTimeMillis() / 1000;
            producer.asyncSend(id).whenComplete((res, ex) -> {
                if(ex != null) {
                    logger.info("[testASyncSend][Mã gửi: [{}], gửi message thất bại]", id, ex);
                } else {
                    logger.info("[testASyncSend][Mã gửi: [{}] gửi thành công, kết quả là: [{}]]", id, res);
                }
            });

            Thread.sleep(10_000L);
        }

        new CountDownLatch(1).await();
    }

}
