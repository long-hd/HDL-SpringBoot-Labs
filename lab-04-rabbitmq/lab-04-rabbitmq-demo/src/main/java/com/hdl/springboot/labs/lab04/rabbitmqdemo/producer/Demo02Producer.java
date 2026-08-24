package com.hdl.springboot.labs.lab04.rabbitmqdemo.producer;

import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo02Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Demo02Producer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void syncSend(Integer id, String routingKey) {
        // Tạo message
        Demo02Message message = new Demo02Message();
        message.setId(id);
        // Gửi
        rabbitTemplate.convertAndSend(Demo02Message.EXCHANGE, routingKey, message);
    }

}
