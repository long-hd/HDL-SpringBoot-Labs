package com.hdl.springboot.labs.lab03.kafkademo.producer;

import com.hdl.springboot.labs.lab03.kafkademo.message.Demo05Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class Demo05Producer {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public CompletableFuture<SendResult<String, Object>> asyncSend(Integer id) {
        Demo05Message message = new Demo05Message();
        message.setId(id);

        return kafkaTemplate.send(Demo05Message.TOPIC, message);
    }

}
