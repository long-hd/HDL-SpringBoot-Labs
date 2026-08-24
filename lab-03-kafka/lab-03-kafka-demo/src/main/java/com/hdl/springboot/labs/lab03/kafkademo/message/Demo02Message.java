package com.hdl.springboot.labs.lab03.kafkademo.message;

public class Demo02Message {

    public static final String TOPIC = "DEMO_02";


    private Integer id;

    public Demo02Message setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Demo02Message{" +
                "id=" + id +
                '}';
    }

}
