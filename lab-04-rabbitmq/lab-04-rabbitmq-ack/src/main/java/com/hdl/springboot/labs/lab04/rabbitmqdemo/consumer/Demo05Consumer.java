package com.hdl.springboot.labs.lab04.rabbitmqdemo.consumer;

import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo05Message;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RabbitListener(queues = Demo05Message.QUEUE)
public class Demo05Consumer {

    Logger logger = LoggerFactory.getLogger(getClass());

    @RabbitHandler
    public void onMessage(Demo05Message message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        logger.info("[onMessage][Thread ID: {}. Nội dung tin nhắn: {}]", Thread.currentThread().getId(), message);

        // Gửi tiến độ tiêu thụ
        if (message.getId() % 2 == 1) {
            // ack - xác nhận tin nhắn
            // Tham số thứ hai `multiple` dùng để xác nhận nhiều tin nhắn cùng lúc.
            // Để giảm lưu lượng mạng, việc xác nhận thủ công có thể được thực hiện theo lô.
            //
            // 1. Khi `multiple` là true, có thể xác nhận một lần tất cả các tin nhắn
            //    có deliveryTag nhỏ hơn hoặc bằng giá trị được truyền vào.
            //
            // 2. Khi `multiple` là false, chỉ xác nhận tin nhắn tương ứng với
            //    deliveryTag hiện tại.
            channel.basicAck(deliveryTag, false);
        }

    }

}
