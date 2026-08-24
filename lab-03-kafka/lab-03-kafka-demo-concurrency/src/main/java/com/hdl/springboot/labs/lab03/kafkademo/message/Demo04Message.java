package com.hdl.springboot.labs.lab03.kafkademo.message;

public class Demo04Message {

    public static final String TOPIC = "DEMO_04";

    private Integer id;

    public Demo04Message setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Demo04Message{" +
                "id=" + id +
                '}';
    }

}
