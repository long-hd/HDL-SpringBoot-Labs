package com.hdl.springboot.labs.labx05.nacosconfig.controller;

import com.hdl.springboot.labs.labx05.nacosconfig.config.OrderProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/demo")
@RefreshScope // @RefreshScope cho @Value
public class DemoController {

    private final OrderProperties orderProperties;

    @Value("${order.pay-timeout-seconds}")
    private Integer payTimeoutSeconds;

    @Value("${order.create-frequency-seconds}")
    private Integer createFrequencySeconds;

    public DemoController(OrderProperties orderProperties) {
        this.orderProperties = orderProperties;
    }

    @GetMapping("/test01")
    public OrderProperties test01() {
        return orderProperties;
    }

    @GetMapping("/test02")
    public Map<String, Integer> test02() {
        return Map.of(
                "payTimeoutSeconds", payTimeoutSeconds,
                "createFrequencySeconds", createFrequencySeconds
        );
    }

}
