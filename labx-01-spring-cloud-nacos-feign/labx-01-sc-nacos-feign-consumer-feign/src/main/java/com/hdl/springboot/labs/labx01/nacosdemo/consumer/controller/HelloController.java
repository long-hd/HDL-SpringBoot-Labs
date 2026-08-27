package com.hdl.springboot.labs.labx01.nacosdemo.consumer.controller;

import com.hdl.springboot.labs.labx01.nacosdemo.consumer.dto.DemoDTO;
import com.hdl.springboot.labs.labx01.nacosdemo.consumer.feign.ProviderFeignClient;
import org.springframework.web.bind.annotation.*;

@RestController
public class HelloController {

    private final ProviderFeignClient providerFeignClient;

    public HelloController(ProviderFeignClient providerFeignClient) {
        this.providerFeignClient = providerFeignClient;
    }

    @GetMapping("/hello")
    public String hello(@RequestParam String name) {
        return "consumer:" + providerFeignClient.echo(name);
    }

    @GetMapping("/get-demo")
    public DemoDTO getDemo(DemoDTO demoDTO) {
        return providerFeignClient.getDemo(demoDTO);
    }
    @PostMapping("/post-demo")
    public DemoDTO postDemo(@RequestBody DemoDTO demoDTO) {
        return providerFeignClient.postDemo(demoDTO);
    }

}
