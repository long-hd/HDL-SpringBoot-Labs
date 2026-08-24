package com.hdl.springboot.labs.lab03.kafkademo.consumer;

import com.hdl.springboot.labs.lab03.kafkademo.message.Demo04Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class Demo04Consumer {

    Logger logger = LoggerFactory.getLogger(Demo04Consumer.class);

    @KafkaListener(topics = Demo04Message.TOPIC,
            groupId = "demo04-consumer-group-" + Demo04Message.TOPIC,
            concurrency = "2")
    public void onMessage(Demo04Message message) {
        logger.info("[onMessage][Thread ID: {}. Nội dung tin nhắn: {}]", Thread.currentThread().getId(), message);
    }

}
