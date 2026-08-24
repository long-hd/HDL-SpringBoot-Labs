package com.hdl.springboot.labs.lab03.kafkademo.message;

public class Demo05Message {

    public static final String TOPIC = "DEMO_05";

    private Integer id;

    public Demo05Message setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Demo05Message{" +
                "id=" + id +
                '}';
    }

}
