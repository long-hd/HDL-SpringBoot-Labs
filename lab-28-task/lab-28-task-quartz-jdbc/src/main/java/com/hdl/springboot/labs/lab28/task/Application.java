package com.hdl.springboot.labs.lab28.task;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point instance 1 — Quartz JDBC cluster.
 *
 * <p>Chạy cùng {@link Application02} (port khác) để verify cluster:
 * cùng {@code scheduler-name}, job không fire trùng trên 2 JVM.
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}