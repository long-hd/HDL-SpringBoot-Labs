package com.hdl.springboot.labs.labx04.sentineldemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** App :8089 — Sentinel trong JVM, Dashboard :7070. Không Nacos, không Redis, không Gateway. */
@SpringBootApplication
public class SentinelDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SentinelDemoApplication.class, args);
    }

}