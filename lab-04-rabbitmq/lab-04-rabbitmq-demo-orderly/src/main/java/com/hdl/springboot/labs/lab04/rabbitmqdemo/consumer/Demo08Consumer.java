package com.hdl.springboot.labs.lab04.rabbitmqdemo.consumer;

import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo08Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = Demo08Message.QUEUE_0)
@RabbitListener(queues = Demo08Message.QUEUE_1)
@RabbitListener(queues = Demo08Message.QUEUE_2)
@RabbitListener(queues = Demo08Message.QUEUE_3)
public class Demo08Consumer {

    Logger logger = LoggerFactory.getLogger(getClass());

    @RabbitHandler(isDefault = true)
    public void onMessage(Message<Demo08Message> message) {
        logger.info("[onMessage][Số hiệu thread:{} Queue:{} Số hiệu message:{}]",
                Thread.currentThread().getId(),
                getQueue(message),
                message.getPayload().getId());

    }

    private String getQueue(Message<Demo08Message> message) {
        return message.getHeaders().get("amqp_consumerQueue", String.class);
    }

}
