package com.hdl.springboot.labs.lab26.distributedsession.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession // Tự động cấu hình Spring Session sử dụng Redis làm nguồn dữ liệu
public class SessionConfig {
}
