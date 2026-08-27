package com.hdl.springboot.labs.labx04.sentinelnacos.web;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.RequestOriginParser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CustomRequestOriginParser implements RequestOriginParser {

    @Override
    public String parseOrigin(HttpServletRequest request) {
        String origin = request.getHeader("s-user");
        if (!StringUtils.hasText(origin)) {
            return "default";
        }
        return origin;
    }

}
