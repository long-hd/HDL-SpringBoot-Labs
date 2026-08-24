package com.hdl.springboot.labs.lab26.distributedsession.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession // Tự động cấu hình Spring Session sử dụng Redis làm nguồn dữ liệu
public class SessionConfig {

//    /**
//     * Tạo bean {@link RedisSerializer} được sử dụng bởi {@link RedisOperationsSessionRepository}.
//     * <p>
//     * Để biết chi tiết, hãy tham khảo phương thức {@link RedisHttpSessionConfiguration#setDefaultRedisSerializer(RedisSerializer)},
//     * phương thức này sử dụng bean có tên là "springSessionDefaultRedisSerializer".
//     *
//     * @return bean RedisSerializer
//     */
//    @Bean(name = "springSessionDefaultRedisSerializer")
//    public RedisSerializer springSessionDefaultRedisSerializer() {
//        return RedisSerializer.json();
//    }

}
