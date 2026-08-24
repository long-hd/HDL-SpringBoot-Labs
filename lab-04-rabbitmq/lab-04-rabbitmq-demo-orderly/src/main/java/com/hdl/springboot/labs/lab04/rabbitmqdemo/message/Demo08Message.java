package com.hdl.springboot.labs.lab04.rabbitmqdemo.message;

import java.io.Serializable;

public class Demo08Message implements Serializable {

    private static final String QUEUE_BASE = "QUEUE_DEMO_08-";
    public static final String QUEUE_0 = QUEUE_BASE + "0";
    public static final String QUEUE_1 = QUEUE_BASE + "1";
    public static final String QUEUE_2 = QUEUE_BASE + "2";
    public static final String QUEUE_3 = QUEUE_BASE + "3";

    public static final int QUEUE_COUNT = 4;

    public static final String EXCHANGE = "EXCHANGE_DEMO_08";

    private Integer id;

    public Demo08Message setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Demo10Message{" +
                "id=" + id +
                '}';
    }

}
