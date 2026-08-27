package com.hdl.springboot.labs.labx01.nacosdemo.provider.controller;

import com.hdl.springboot.labs.labx01.nacosdemo.provider.dto.DemoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProviderController {

    Logger log = LoggerFactory.getLogger(getClass());

    @Value("${server.port}")
    private Integer serverPort;

    @GetMapping("/echo")
    public String echo(@RequestParam String name) {
        log.info("[echo] name={}", name);
        return serverPort + "-provider:" + name;
    }

    @GetMapping("/get_demo")
    public DemoDTO getDemo(DemoDTO demoDTO) {
        return demoDTO;
    }

    @PostMapping("/post_demo")
    public DemoDTO postDemo(@RequestBody DemoDTO demoDTO) {
        return demoDTO;
    }

}
