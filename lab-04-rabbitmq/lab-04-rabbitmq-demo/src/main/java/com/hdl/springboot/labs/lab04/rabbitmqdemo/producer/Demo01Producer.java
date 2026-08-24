package com.hdl.springboot.labs.lab04.rabbitmqdemo.producer;

import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo01Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class Demo01Producer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void syncSend(Integer id) {
        // Tạo message
        Demo01Message message = new Demo01Message();
        message.setId(id);

        // Gửi message theo phương thức đồng bộ
        rabbitTemplate.convertAndSend(Demo01Message.EXCHANGE, Demo01Message.ROUTING_KEY, message);
    }

    public void syncSendDefault(Integer id) {
        Demo01Message message = new Demo01Message();
        message.setId(id);

        rabbitTemplate.convertAndSend(Demo01Message.QUEUE, message);
    }

    @Async
    public CompletableFuture<Void> asyncSend(Integer id) {
        try {
            this.syncSend(id);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

}
