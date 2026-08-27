package com.hdl.springboot.labs.labx04.sentineldemo.web;


import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** JSON khi URL bị Sentinel chặn. code 1024 = số yudao (không phải HTTP status). */
@RestControllerAdvice(basePackages = "com.hdl.springboot.labs.labx04.sentineldemo")
public class GlobalExceptionHandler {

    @ExceptionHandler(BlockException.class)
    public Map<String, Object> blockExceptionHandler(BlockException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 1024);
        body.put("msg", "blocked: " + ex.getClass().getSimpleName());
        return body;
    }

}
