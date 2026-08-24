package com.hdl.springboot.labs.lab11.springdatarediswithlettuce.config;

import com.hdl.springboot.labs.lab11.springdatarediswithlettuce.listener.TestChannelTopicMessageListener;
import com.hdl.springboot.labs.lab11.springdatarediswithlettuce.listener.TestPatternTopicMessageListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfiguration {

    @Bean
    RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        template.setEnableTransactionSupport(true);

        template.setConnectionFactory(connectionFactory);

        var stringSerializer = new StringRedisSerializer();
        var jsonSerializer = new GenericJackson2JsonRedisSerializer();

        // Use String serialization for keys.
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Use JSON serialization (Jackson) for values.
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        // Create a RedisMessageListenerContainer object
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();

        // Set the RedisConnection factory. This is the secret factory that enables integration with various Java Redis clients.
        container.setConnectionFactory(connectionFactory);

        // Add a listener
        container.addMessageListener(new TestChannelTopicMessageListener(), new ChannelTopic("TEST"));
        container.addMessageListener(new TestChannelTopicMessageListener(), new ChannelTopic("TEST2"));
        container.addMessageListener(new TestPatternTopicMessageListener(), new ChannelTopic("TEST"));

        return container;
    }

}
