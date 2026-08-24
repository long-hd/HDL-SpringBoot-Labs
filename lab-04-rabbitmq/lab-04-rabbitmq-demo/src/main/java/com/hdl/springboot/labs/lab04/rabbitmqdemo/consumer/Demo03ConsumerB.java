package com.hdl.springboot.labs.lab04.rabbitmqdemo.consumer;

import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo03Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@RabbitListener(queues = Demo03Message.QUEUE_B)
public class Demo03ConsumerB implements Serializable {

    Logger logger = LoggerFactory.getLogger(getClass());

    @RabbitHandler
    public void onMessage(Demo03Message message) {
        logger.info("[onMessage][Thread ID: {}. Nội dung tin nhắn: {}]", Thread.currentThread().getId(), message);
    }

}
