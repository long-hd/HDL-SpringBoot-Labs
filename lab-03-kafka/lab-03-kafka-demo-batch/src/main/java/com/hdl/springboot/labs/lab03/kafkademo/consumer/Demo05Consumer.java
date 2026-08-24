package com.hdl.springboot.labs.lab03.kafkademo.consumer;

import com.hdl.springboot.labs.lab03.kafkademo.message.Demo05Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class Demo05Consumer {

    Logger logger = LoggerFactory.getLogger(getClass());

    @KafkaListener(topics = Demo05Message.TOPIC,
            groupId = "demo05-consumer-group-" + Demo05Message.TOPIC)
    public void onMessage(Demo05Message message) {
        logger.info("[onMessage][Thread ID: {}. Nội dung tin nhắn: {}]", Thread.currentThread().getId(), message);
    }

}
