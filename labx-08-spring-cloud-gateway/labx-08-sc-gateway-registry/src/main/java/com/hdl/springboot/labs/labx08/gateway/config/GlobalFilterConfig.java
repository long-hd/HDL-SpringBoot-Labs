package com.hdl.springboot.labs.labx08.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

@Configuration
public class GlobalFilterConfig {

    private static final Logger log = LoggerFactory.getLogger(GlobalFilterConfig.class);

    @Bean
    @Order(0)
    public GlobalFilter loggingGlobalFilter() {
        return (exchange, chain) -> {
            log.info("[gateway][pre] {} {}",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getURI().getPath());
            return chain.filter(exchange)
                    .then(Mono.fromRunnable(() ->
                            log.info("[gateway][post] status={}",
                                    exchange.getResponse().getStatusCode())));
        };
    }

}
