package com.hdl.springboot.labs.lab04.rabbitmqdemo.config;

import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo01Message;
import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo02Message;
import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo03Message;
import com.hdl.springboot.labs.lab04.rabbitmqdemo.message.Demo04Message;
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

    /**
     * Lớp cấu hình cho ví dụ về Direct Exchange
     */
    public static class DirectExchangeDemoConfig {

        // Tạo hàng đợi
        @Bean
        public Queue demo01Queue() {
            return new Queue(Demo01Message.QUEUE, // Tên hàng đợi
                    true, // durable: có lưu trữ bền vững hay không
                    false, // exclusive: có độc quyền hay không
                    false); // autoDelete: có tự động xóa hay không
        }

        // Tạo Direct Exchange
        @Bean
        public DirectExchange demo01Exchange() {
            return new DirectExchange(Demo01Message.EXCHANGE,
                    true, // durable: có lưu trữ lâu dài hay không
                    false); // exclusive: có độc quyền hay không
        }

        // Tạo Binding Key
        @Bean
        public Binding demo01Binding() {
            return BindingBuilder.bind(demo01Queue())
                    .to(demo01Exchange())
                    .with(Demo01Message.ROUTING_KEY);
        }

    }

    /**
     * Lớp cấu hình cho ví dụ về Topic Exchange
     */
    public static class TopicExchangeDemoConfig {

        // Tạo hàng đợi
        @Bean
        public Queue demo02Queue() {
            return new Queue(Demo02Message.QUEUE, // Tên hàng đợi
                    true, // durable: có lưu trữ bền vững hay không
                    false, // exclusive: có độc quyền hay không
                    false); // autoDelete: có tự động xóa hay không
        }

        // Tạo Direct Exchange
        @Bean
        public TopicExchange demo02Exchange() {
            return new TopicExchange(Demo02Message.EXCHANGE,
                    true, // durable: có lưu trữ lâu dài hay không
                    false); // exclusive: có độc quyền hay không
        }

        // Tạo Binding Key
        @Bean
        public Binding demo02Binding() {
            return BindingBuilder.bind(demo02Queue())
                    .to(demo02Exchange())
                    .with(Demo02Message.ROUTING_KEY);
        }

    }

    /**
     * Lớp cấu hình cho ví dụ về Fanout Exchange
     */
    public static class FanoutExchangeDemoConfig {

        // Tạo hàng đợi
        @Bean
        public Queue demo03QueueA() {
            return new Queue(Demo03Message.QUEUE_A, // Tên hàng đợi
                    true, // durable: có lưu trữ bền vững hay không
                    false, // exclusive: có độc quyền hay không
                    false); // autoDelete: có tự động xóa hay không
        }

        @Bean
        public Queue demo03QueueB() {
            return new Queue(Demo03Message.QUEUE_B, // Tên hàng đợi
                    true, // durable: có lưu trữ bền vững hay không
                    false, // exclusive: có độc quyền hay không
                    false); // autoDelete: có tự động xóa hay không
        }

        // Tạo Direct Exchange
        @Bean
        public FanoutExchange demo03Exchange() {
            return new FanoutExchange(Demo03Message.EXCHANGE,
                    true, // durable: có lưu trữ lâu dài hay không
                    false); // exclusive: có độc quyền hay không
        }

        // Tạo Binding Key
        @Bean
        public Binding demo03BindingA() {
            return BindingBuilder.bind(demo03QueueA())
                    .to(demo03Exchange());
        }

        @Bean
        public Binding demo03BindingB() {
            return BindingBuilder.bind(demo03QueueB())
                    .to(demo03Exchange());
        }

    }

    /**
     * Lớp cấu hình cho ví dụ về Header Exchange
     */
    public static class HeadersExchangeDemoConfig {

        // Tạo hàng đợi
        @Bean
        public Queue demo04Queue() {
            return new Queue(Demo04Message.QUEUE, // Tên hàng đợi
                    true, // durable: có lưu trữ bền vững hay không
                    false, // exclusive: có độc quyền hay không
                    false); // autoDelete: có tự động xóa hay không
        }

        // Tạo Direct Exchange
        @Bean
        public HeadersExchange demo04Exchange() {
            return new HeadersExchange(Demo04Message.EXCHANGE,
                    true, // durable: có lưu trữ lâu dài hay không
                    false); // exclusive: có độc quyền hay không
        }

        // Tạo Binding Key
        @Bean
        public Binding demo04Binding() {
            return BindingBuilder.bind(demo04Queue()).to(demo04Exchange())
                    .where(Demo04Message.HEADER_KEY).matches(Demo04Message.HEADER_VALUE);
        }

    }

}
