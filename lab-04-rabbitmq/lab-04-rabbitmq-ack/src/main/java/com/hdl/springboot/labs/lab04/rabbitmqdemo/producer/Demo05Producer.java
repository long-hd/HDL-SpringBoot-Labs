package com.hdl.springboot.labs.lab04.rabbitmqdemo.producer;

import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo05Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Demo05Producer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void syncSend(Integer id) {
        Demo05Message message = new Demo05Message();
        message.setId(id);

        rabbitTemplate.convertAndSend(Demo05Message.EXCHANGE, Demo05Message.ROUTING_KEY, message);
    }

}
