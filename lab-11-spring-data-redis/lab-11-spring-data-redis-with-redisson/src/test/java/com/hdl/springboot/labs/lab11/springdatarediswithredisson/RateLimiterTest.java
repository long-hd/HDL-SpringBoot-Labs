package com.hdl.springboot.labs.lab11.springdatarediswithredisson;

import org.junit.jupiter.api.Test;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

@SpringBootTest
public class RateLimiterTest {

    @Autowired
    private RedissonClient redissonClient;

    @Test
    public void test() throws InterruptedException {
        // Tạo một đối tượng RRateLimiter
        RRateLimiter rateLimiter = redissonClient.getRateLimiter("rateLimiter");

        // Khởi tạo: Tốc độ dòng tối đa = 2 token được tạo mỗi giây
        rateLimiter.trySetRate(RateType.OVERALL, 2, Duration.ofSeconds(1));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < 6; i++) {
            System.out.printf("%s: Kết quả giành quyền khóa (%s)\n", sdf.format(new Date()), rateLimiter.tryAcquire());
            Thread.sleep(250L);
        }
    }

}
