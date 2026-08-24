package com.hdl.springboot.labs.lab04.rabbitmqdemo.producer;

import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo03Message;
import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo04Message;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Demo04Producer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void syncSend(Integer id, String headerValue) {
        // Header
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader(Demo04Message.HEADER_KEY, headerValue);

        // Message
        Demo04Message body = new Demo04Message();
        body.setId(id);

        Message message = rabbitTemplate.getMessageConverter().toMessage(body, messageProperties);

        // Gửi
        rabbitTemplate.convertAndSend(Demo04Message.EXCHANGE, null, message);
    }

}
