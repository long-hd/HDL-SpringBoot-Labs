package com.hdl.springboot.labs.lab04.rabbitmqdemo.producer;

import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo07Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Demo07Producer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void syncSend(Integer id) {
        Demo07Message message = new Demo07Message();
        message.setId(id);

        rabbitTemplate.convertAndSend(Demo07Message.EXCHANGE, Demo07Message.ROUTING_KEY, message);
    }

}
