package com.hdl.springboot.labs.lab04.rabbitmqdemo.consumer;

import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo07Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
//@RabbitListener(queues = Demo07Message.QUEUE)
@RabbitListener(queues = Demo07Message.QUEUE, concurrency = "2")
//@RabbitListener(queues = {"QUEUE_DEMO_01", "QUEUE_DEMO_02"})
public class Demo07Consumer {

    Logger logger = LoggerFactory.getLogger(getClass());

    @RabbitHandler
    public void onMessage(Demo07Message message) {
        logger.info("[onMessage][Thread ID: {}. Nội dung tin nhắn: {}]", Thread.currentThread().getId(), message);
    }

}
