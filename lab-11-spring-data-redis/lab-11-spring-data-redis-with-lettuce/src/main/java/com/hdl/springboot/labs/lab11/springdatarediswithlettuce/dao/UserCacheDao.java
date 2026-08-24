package com.hdl.springboot.labs.lab11.springdatarediswithlettuce.dao;

import com.hdl.springboot.labs.lab11.springdatarediswithlettuce.cacheobject.UserCacheObject;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserCacheDao {

    private static final String KEY_PATTERN = "user:%d";

    private final RedisTemplate<String, Object> redisTemplate;

    public UserCacheDao(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String buildKey(Integer id) {
        return String.format(KEY_PATTERN, id);
    }

    public UserCacheObject get(Integer id) {
        String key = buildKey(id);
        Object value = redisTemplate.opsForValue().get(key);
        return value instanceof UserCacheObject user ? user : null;
    }

    public void set(Integer id, UserCacheObject userCacheObject) {
        String key = buildKey(id);
        redisTemplate.opsForValue().set(key, userCacheObject);
    }

}
