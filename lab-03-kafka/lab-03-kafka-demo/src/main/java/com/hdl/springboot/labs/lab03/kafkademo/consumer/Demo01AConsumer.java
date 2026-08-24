package com.hdl.springboot.labs.lab03.kafkademo.consumer;

import com.hdl.springboot.labs.lab03.kafkademo.message.Demo01Message;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class Demo01AConsumer {

    Logger logger = LoggerFactory.getLogger(getClass());

    @KafkaListener(topics = Demo01Message.TOPIC,
            groupId = "demo01-A-consumer-group-" + Demo01Message.TOPIC)
    public void onMessageA(ConsumerRecord<?, ?> record) {
        logger.info("[onMessage - A][Thread ID: {}. Nội dung tin nhắn: {}]", Thread.currentThread().getId(), record);
    }

    @KafkaListener(topics = Demo01Message.TOPIC,
            groupId = "demo01-B-consumer-group-" + Demo01Message.TOPIC)
    public void onMessageB(ConsumerRecord<String, Demo01Message> record) {
        logger.info("[onMessage - B][Thread ID: {}. Nội dung tin nhắn: {}]", Thread.currentThread().getId(), record);
    }

}
