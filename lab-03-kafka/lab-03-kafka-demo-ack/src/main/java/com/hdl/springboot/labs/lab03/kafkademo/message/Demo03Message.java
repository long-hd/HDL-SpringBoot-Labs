package com.hdl.springboot.labs.lab03.kafkademo.message;

public class Demo03Message {

    public static final String TOPIC = "DEMO_03";

    private Integer id;

    public Demo03Message setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Demo03Message{" +
                "id=" + id +
                '}';
    }

}
