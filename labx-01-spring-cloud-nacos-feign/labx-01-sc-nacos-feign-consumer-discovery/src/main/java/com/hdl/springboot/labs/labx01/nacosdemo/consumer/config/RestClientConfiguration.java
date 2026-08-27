package com.hdl.springboot.labs.labx01.nacosdemo.consumer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfiguration {

    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }

}
