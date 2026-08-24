package com.hdl.springboot.labs.lab04.rabbitmqdemo.consumer;

import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo06Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = Demo06Message.DEAD_QUEUE)
public class Demo06DeadConsumer {

    Logger logger = LoggerFactory.getLogger(getClass());

    @RabbitHandler
    public void onMessage(Demo06Message message) {
        logger.info("[onMessage][【Hàng đợi thư chết】Thread ID:{} Nội dung tin nhắn:{}]",
                Thread.currentThread().getId(), message);
    }

}
