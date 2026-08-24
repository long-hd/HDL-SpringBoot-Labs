package com.hdl.springboot.labs.lab11.springdatarediswithredisson;

import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class LockTest {

    private static final String LOCK_KEY = "anylock";

    @Autowired
    private RedissonClient redissonClient;

    @Test
    public void test() throws InterruptedException {
        // Khởi động luồng A để chiếm giữ khóa
        new Thread(() -> {
            // Khóa sẽ tự động được giải phóng sau 10 giây
            // Không cần gọi thủ công phương thức mở khóa
            final RLock lock = redissonClient.getLock(LOCK_KEY);
            lock.lock(10, TimeUnit.SECONDS);
        }).start();
        // Chỉ cần tạm dừng (sleep) 1 giây để đảm bảo luồng A đã chiếm giữ khóa thành công
        Thread.sleep(1000L);

        // Cố gắng giành quyền khóa, chờ tối đa 100 giây; tự động giải phóng khóa 10 giây sau khi giành được.
        System.out.printf("Chuẩn bị lấy thời gian khóa: %s%n", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        final RLock lock = redissonClient.getLock(LOCK_KEY);
        boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
        if(res) {
            System.out.printf("Thời gian thực tế để giành được khóa: %s", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        } else {
            System.out.println("Không thể giành được khóa");
        }
    }

}
