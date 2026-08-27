package com.hdl.springboot.labs.labx08.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
            if(remote == null || remote.getAddress() == null) {
                return Mono.empty();
            }
            return Mono.just(remote.getAddress().getHostAddress());
        };
    }

}
