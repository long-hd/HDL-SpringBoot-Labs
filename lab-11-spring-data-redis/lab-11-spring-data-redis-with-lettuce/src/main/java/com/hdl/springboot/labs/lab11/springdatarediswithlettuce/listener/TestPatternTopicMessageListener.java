package com.hdl.springboot.labs.lab11.springdatarediswithlettuce.listener;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

public class TestPatternTopicMessageListener implements MessageListener {

    @Override
    public void onMessage(Message message, byte[] pattern) {
        System.out.println("Received PatternTopic message: ");
        System.out.println("Thread ID: " + Thread.currentThread().getName());
        System.out.println("Message: " + message);
        System.out.println("Pattern: " + new String(pattern));
    }

}
