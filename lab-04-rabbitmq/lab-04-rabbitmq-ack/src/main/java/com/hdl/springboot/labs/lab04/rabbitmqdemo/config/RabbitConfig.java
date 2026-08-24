package com.hdl.springboot.labs.lab04.rabbitmqdemo.config;

import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo05Message;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    public static class DirectExchangeConfig {

        // Tạo hàng đợi
        @Bean
        public Queue demo05Queue() {
            return new Queue(Demo05Message.QUEUE, // Tên hàng đợi
                    true, // durable: có lưu trữ bền vững hay không
                    false, // exclusive: có độc quyền hay không
                    false); // autoDelete: có tự động xóa hay không
        }

        // Tạo Direct Exchange
        @Bean
        public DirectExchange demo05Exchange() {
            return new DirectExchange(Demo05Message.EXCHANGE,
                    true, // durable: có lưu trữ lâu dài hay không
                    false); // exclusive: có độc quyền hay không
        }

        // Tạo Binding Key
        @Bean
        public Binding demo05Binding() {
            return BindingBuilder.bind(demo05Queue())
                    .to(demo05Exchange())
                    .with(Demo05Message.ROUTING_KEY);
        }

    }

}
