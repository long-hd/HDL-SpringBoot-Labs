package com.hdl.springboot.labs.lab03.kafkademo.producer;

import com.hdl.springboot.labs.lab03.kafkademo.message.Demo04Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class Demo04Producer {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public SendResult syncSend(Integer id) throws ExecutionException, InterruptedException {
        Demo04Message message = new Demo04Message();
        message.setId(id);

        return kafkaTemplate.send(Demo04Message.TOPIC, message).get();
    }

    public SendResult syncSendOrderly(Integer id) throws ExecutionException, InterruptedException {
        Demo04Message message = new Demo04Message();
        message.setId(id);

        // Gửi message đồng bộ
        // Vì chúng ta sử dụng kiểu String để serialize key, nên cần chuyển id thành String
        return kafkaTemplate.send(Demo04Message.TOPIC, String.valueOf(id), message).get();
    }

}
