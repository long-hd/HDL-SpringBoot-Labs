package com.hdl.springboot.labs.lab28.task;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point instance 2 — cùng cluster với {@link Application}.
 *
 * <p>Random port tránh trùng Tomcat khi chạy 2 process trên máy local.
 */
@SpringBootApplication
public class Application02 {

    public static void main(String[] args) {
        System.setProperty("server.port", "18081");
        SpringApplication.run(Application.class, args);
    }

}
