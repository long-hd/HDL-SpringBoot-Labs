package com.hdl.springboot.labs.lab11.springdatarediswithlettuce;

import com.hdl.springboot.labs.lab11.springdatarediswithlettuce.cacheobject.UserCacheObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
public class Test01 {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    public void testStringSetKey() {
        stringRedisTemplate.opsForValue().set("key1", "value1");
    }

    @Test
    public void testStringSetKey02() {
        redisTemplate.opsForValue().set("key2", "value2");
    }

    @Test
    public void testSetAdd() {
        stringRedisTemplate.opsForSet().add("set1", "set2", "set3");
    }

    @Test
    public void testStringSetKeyUserCache() {
        UserCacheObject object = new UserCacheObject()
                .setId(1)
                .setName("user name")
                .setGender(1);
        String key = String.format("user:%d",  object.getId());
        redisTemplate.opsForValue().set(key, object);
    }

    @Test
    public void testStringGetKeyUserCache() {
        String key = String.format("user:%d",  1);
        Object value = redisTemplate.opsForValue().get(key);
        System.out.println(value);
    }

}
