package com.hdl.springboot.labs.lab03.kafkademo.producer;

import com.hdl.springboot.labs.lab03.kafkademo.message.Demo03Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class Demo03Producer {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public SendResult syncSend(Integer id) throws ExecutionException, InterruptedException {
        Demo03Message message = new Demo03Message();
        message.setId(id);

        return kafkaTemplate.send(Demo03Message.TOPIC, message).get();
    }

}
