package com.hdl.springboot.labs.labx01.nacosdemo.consumer.feign;

import com.hdl.springboot.labs.labx01.nacosdemo.consumer.dto.DemoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "demo-provider")
public interface ProviderFeignClient {

    @GetMapping("/echo")
    String echo(@RequestParam("name") String name);

    @GetMapping("/get_demo")
    DemoDTO getDemo(@SpringQueryMap DemoDTO dto);

    @PostMapping("/post_demo")
    DemoDTO postDemo(@RequestBody DemoDTO dto);

}
