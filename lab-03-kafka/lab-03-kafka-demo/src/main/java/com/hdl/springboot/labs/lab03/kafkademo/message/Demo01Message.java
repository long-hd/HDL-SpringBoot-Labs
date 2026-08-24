package com.hdl.springboot.labs.lab03.kafkademo.message;

public class Demo01Message {

    public static final String TOPIC = "DEMO_01";


    private Integer id;

    public Demo01Message setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Demo01Message{" +
                "id=" + id +
                '}';
    }

}
