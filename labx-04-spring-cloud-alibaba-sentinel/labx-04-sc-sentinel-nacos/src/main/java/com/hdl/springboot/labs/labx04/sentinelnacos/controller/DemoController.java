package com.hdl.springboot.labs.labx04.sentinelnacos.controller;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
public class DemoController {

    @GetMapping("/echo")
    public String echo() {
        return "echo";
    }
    @GetMapping("/test")
    public String test() {
        return "test";
    }

    @GetMapping("/sleep")
    public String sleep() throws InterruptedException {
        Thread.sleep(100L);
        return "sleep";
    }
    @GetMapping("/product_info")
    @SentinelResource("demo_product_info_hot")
    public String productInfo(Integer id) {
        return "product id=" + id;
    }

    @GetMapping("/entry_demo")
    public String entryDemo() {
        Entry entry = null;
        try {
            entry = SphU.entry("entry_demo");
            return "ok";
        } catch (BlockException ex) {
            return "blocked:" + ex.getClass().getSimpleName();
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    @GetMapping("/annotations_demo")
    @SentinelResource(value = "annotations_demo_resource",
            blockHandler = "blockHandler",
            fallback = "fallback")
    public String annotationsDemo(@RequestParam(required = false) Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("id required");
        }
        return "success";
    }

    public String blockHandler(Integer id, BlockException ex) {
        return "block:" + ex.getClass().getSimpleName();
    }
    public String fallback(Integer id, Throwable throwable) {
        return "fallback:" + throwable.getMessage();
    }

}
