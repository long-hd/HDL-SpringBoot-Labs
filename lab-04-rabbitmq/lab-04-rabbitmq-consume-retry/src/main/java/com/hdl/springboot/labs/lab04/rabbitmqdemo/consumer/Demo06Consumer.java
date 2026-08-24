package com.hdl.springboot.labs.lab04.rabbitmqdemo.consumer;

import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo06Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = Demo06Message.QUEUE)
public class Demo06Consumer {

    Logger logger = LoggerFactory.getLogger(getClass());

    @RabbitHandler
    public void onMessage(Demo06Message message) {
        logger.info("[onMessage][Thread ID: {}. Nội dung tin nhắn: {}]", Thread.currentThread().getId(), message);
        // <X> Lưu ý: tại đây cố tình ném ra một RuntimeException để mô phỏng việc tiêu thụ (xử lý) tin nhắn thất bại
        throw new RuntimeException("Tôi cố tình ném ra một ngoại lệ");

    }

}
