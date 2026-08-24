package com.hdl.springboot.labs.lab04.rabbitmqdemo.message;

import java.io.Serializable;

public class Demo07Message implements Serializable {
    public static final String QUEUE = "QUEUE_DEMO_07";

    public static final String EXCHANGE = "EXCHANGE_DEMO_07";

    public static final String ROUTING_KEY = "ROUTING_KEY_07";

    private Integer id;

    public Demo07Message setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Demo07Message{" +
                "id=" + id +
                '}';
    }

}
