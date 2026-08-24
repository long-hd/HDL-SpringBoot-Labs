package com.hdl.springboot.labs.lab03.kafkademo.consumer;

import com.hdl.springboot.labs.lab03.kafkademo.message.Demo03Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class Demo03Consumer {

    Logger logger = LoggerFactory.getLogger(Demo03Consumer.class);

    @KafkaListener(topics = Demo03Message.TOPIC,
            groupId = "demo03-consumer-group-" + Demo03Message.TOPIC)
    public void onMessage(Demo03Message message, Acknowledgment ack) {
        logger.info("[onMessage][Thread ID: {}. Nội dung tin nhắn: {}]", Thread.currentThread().getId(), message);
        // Commit offset đã tiêu thụ
        if(message.getId() % 2 == 1) {
            ack.acknowledge();
        }
    }

}
