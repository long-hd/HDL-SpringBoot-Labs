package com.hdl.springboot.labs.lab04.rabbitmqdemo.config;

import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo06Message;
import org.springframework.amqp.core.*;
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

        // Queue
        @Bean
        public Queue demo06Queue() {
            return QueueBuilder.durable(Demo06Message.QUEUE)
                    .exclusive()
                    .autoDelete()
                    .deadLetterExchange(Demo06Message.EXCHANGE)
                    .deadLetterRoutingKey(Demo06Message.DEAD_ROUTING_KEY)
                    .build();
        }

        // Dead Queue
        @Bean
        public Queue demo06DeadQueue() {
            return new Queue(Demo06Message.DEAD_QUEUE,
                    true,
                    false,
                    false);
        }

        // Tạo Direct Exchange
        @Bean
        public DirectExchange demo06Exchange() {
            return new DirectExchange(Demo06Message.EXCHANGE,
                    true, // durable: có lưu trữ lâu dài hay không
                    false); // exclusive: có độc quyền hay không
        }

        // Tạo Binding
        @Bean
        public Binding demo06Binding() {
            return BindingBuilder.bind(demo06Queue())
                    .to(demo06Exchange())
                    .with(Demo06Message.ROUTING_KEY);
        }

        // Tạo Dead Binding
        @Bean
        public Binding demo06DeadBinding() {
            return BindingBuilder.bind(demo06DeadQueue())
                    .to(demo06Exchange())
                    .with(Demo06Message.DEAD_ROUTING_KEY);
        }

    }

}
