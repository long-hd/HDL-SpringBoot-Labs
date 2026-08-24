package com.hdl.springboot.labs.lab03.kafkademo.producer;

import com.hdl.springboot.labs.lab03.kafkademo.message.Demo02Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class Demo02Producer {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public SendResult<String, Object> syncSend(Integer id) throws ExecutionException, InterruptedException {
        Demo02Message message = new Demo02Message();
        message.setId(id);

        return kafkaTemplate.send(Demo02Message.TOPIC, message).get();
    }

}
