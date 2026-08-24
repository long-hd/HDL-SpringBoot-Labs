package com.hdl.springboot.labs.lab04.rabbitmqdemo.message;

public class Demo06Message {

    public static final String QUEUE = "QUEUE_DEMO_06"; // Hàng đợi thông thường
    public static final String DEAD_QUEUE = "DEAD_QUEUE_DEMO_06"; // Hàng đợi thư chết (Dead Letter Queue)

    public static final String EXCHANGE = "EXCHANGE_DEMO_06";

    public static final String ROUTING_KEY = "ROUTING_KEY_06"; // Routing key thông thường
    public static final String DEAD_ROUTING_KEY = "DEAD_ROUTING_KEY_06"; // Routing key cho thư chết (Dead Letter)


    /**
     * Mã số
     */
    private Integer id;

    public Demo06Message setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Demo06Message{" +
                "id=" + id +
                '}';
    }


}
