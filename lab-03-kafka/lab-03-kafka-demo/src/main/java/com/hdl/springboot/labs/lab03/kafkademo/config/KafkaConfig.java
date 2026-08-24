package com.hdl.springboot.labs.lab03.kafkademo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<?, ?> kafkaTemplate) {
        // Spring Kafka: hết retry thì publish record lỗi sang topic DLT (thường là <topic>.DLT)
        ConsumerRecordRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        // interval 10s giữa các lần thử; maxAttempts=3 (số lần retry theo FixedBackOff)
        BackOff backOff = new FixedBackOff(10_000L, 3L);
        // Spring Kafka: listener throw → backoff/retry → hết lần thì gọi recoverer (DLT)
        return new DefaultErrorHandler(recoverer, backOff);
    }

}
