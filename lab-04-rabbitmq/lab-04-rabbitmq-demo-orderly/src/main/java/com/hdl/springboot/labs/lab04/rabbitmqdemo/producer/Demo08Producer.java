package com.hdl.springboot.labs.lab04.rabbitmqdemo.producer;

import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo08Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Demo08Producer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void syncSend(Integer id) {
        Demo08Message demo08Message = new Demo08Message();
        demo08Message.setId(id);

        rabbitTemplate.convertAndSend(Demo08Message.EXCHANGE, this.getRoutingKey(id), demo08Message);
    }

    private String getRoutingKey(Integer id) {
        return String.valueOf(id % Demo08Message.QUEUE_COUNT);
    }

}
