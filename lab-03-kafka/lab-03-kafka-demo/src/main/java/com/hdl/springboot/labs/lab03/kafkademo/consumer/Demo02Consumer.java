package com.hdl.springboot.labs.lab03.kafkademo.consumer;

import com.hdl.springboot.labs.lab03.kafkademo.message.Demo01Message;
import com.hdl.springboot.labs.lab03.kafkademo.message.Demo02Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class Demo02Consumer {

    Logger logger = LoggerFactory.getLogger(getClass());

    @KafkaListener(topics = Demo02Message.TOPIC,
            groupId = "demo02-consumer-group-" + Demo02Message.TOPIC)
    public void onMessage(Demo02Message message) {
        logger.info("[onMessage][Thread ID: {}. Nội dung tin nhắn: {}]", Thread.currentThread().getId(), message);

        throw new RuntimeException("Tôi cố tình ném ra lỗi ở đây");
    }

}
