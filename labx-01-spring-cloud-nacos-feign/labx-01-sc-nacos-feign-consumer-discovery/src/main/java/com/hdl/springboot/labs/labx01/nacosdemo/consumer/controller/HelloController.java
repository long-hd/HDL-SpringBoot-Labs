package com.hdl.springboot.labs.labx01.nacosdemo.consumer.controller;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
public class HelloController {

    private static final String PROVIDER_SERVICE = "demo-provider";

    private final DiscoveryClient discoveryClient;
    private final LoadBalancerClient loadBalancerClient;
    private final RestClient restClient;

    public HelloController(DiscoveryClient discoveryClient,
                           LoadBalancerClient loadBalancerClient,
                           RestClient restClient) {
        this.discoveryClient = discoveryClient;
        this.loadBalancerClient = loadBalancerClient;
        this.restClient = restClient;
    }

    @GetMapping("/hello")
    public String hello(@RequestParam String name) {
        List<ServiceInstance> instances = discoveryClient.getInstances(PROVIDER_SERVICE);
        ServiceInstance instance = instances.isEmpty() ? null : instances.get(0);
        return callEcho(instance, name);
    }

    @GetMapping("/hello-lb")
    public String heloLb(@RequestParam String name) {
        ServiceInstance instance = loadBalancerClient.choose(PROVIDER_SERVICE);
        return callEcho(instance, name);
    }

    private String callEcho(ServiceInstance instance, String name) {
        if (instance == null) {
            throw new IllegalStateException("Không có instance demo-provider trên Nacos");
        }

        URI echoUri = UriComponentsBuilder.fromUri(instance.getUri())
                .path("/echo")
                .queryParam("name", name)
                .build(true)
                .toUri();
        String body = restClient.get()
                .uri(echoUri)
                .retrieve()
                .body(String.class);
        return "consumer:" + body;
    }

}
