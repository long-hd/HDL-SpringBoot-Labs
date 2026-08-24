package com.hdl.springboot.labs.lab04.rabbitmqdemo.config;

import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo08Message;
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

    /**
     * Lớp cấu hình cho ví dụ về Direct Exchange
     */
    public static class DirectExchangeDemoConfig {

        // Queue
        @Bean
        public Queue demo08Queue0() {
            return new Queue(Demo08Message.QUEUE_0);
        }

        @Bean
        public Queue demo08Queue1() {
            return new Queue(Demo08Message.QUEUE_1);
        }

        @Bean
        public Queue demo08Queue2() {
            return new Queue(Demo08Message.QUEUE_2);
        }

        @Bean
        public Queue demo08Queue3() {
            return new Queue(Demo08Message.QUEUE_3);
        }

        // Tạo Direct Exchange
        @Bean
        public DirectExchange demo08Exchange() {
            return new DirectExchange(Demo08Message.EXCHANGE,
                    true, // durable: có lưu trữ lâu dài hay không
                    false); // exclusive: có độc quyền hay không
        }

        // Tạo Binding
        @Bean
        public Binding demo08Binding0() {
            return BindingBuilder.bind(demo08Queue0())
                    .to(demo08Exchange())
                    .with("0");
        }

        @Bean
        public Binding demo08Binding1() {
            return BindingBuilder.bind(demo08Queue1())
                    .to(demo08Exchange())
                    .with("1");
        }

        @Bean
        public Binding demo08Binding2() {
            return BindingBuilder.bind(demo08Queue2())
                    .to(demo08Exchange())
                    .with("2");
        }

        @Bean
        public Binding demo08Binding3() {
            return BindingBuilder.bind(demo08Queue3())
                    .to(demo08Exchange())
                    .with("3");
        }

    }

}
