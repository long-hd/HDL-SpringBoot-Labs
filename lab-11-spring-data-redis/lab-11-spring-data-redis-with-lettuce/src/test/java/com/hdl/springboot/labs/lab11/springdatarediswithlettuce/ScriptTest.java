package com.hdl.springboot.labs.lab11.springdatarediswithlettuce;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ScriptTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void test01() throws IOException {
        ClassPathResource resource = new ClassPathResource("lua/compareAndSet.lua");
        String scriptContents = resource.getContentAsString(StandardCharsets.UTF_8);

        RedisScript<Long> redisScript = new DefaultRedisScript<>(scriptContents, Long.class);

        stringRedisTemplate.opsForValue().set("key:1", "expect_value");

        Long results = stringRedisTemplate.execute(redisScript,
                Collections.singletonList("key:1"),
                "expect_value",
                "new_value");
        assertEquals(1L, results);
    }

}
