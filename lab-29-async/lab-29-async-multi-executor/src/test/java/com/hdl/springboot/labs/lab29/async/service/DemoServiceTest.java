package com.hdl.springboot.labs.lab29.async.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class DemoServiceTest {

    @Autowired
    private DemoService demoService;

    @Test
    public void testExecute() throws InterruptedException {
        demoService.execute01();
        demoService.execute02();

        // Tạm dừng 1 giây để đảm bảo lệnh gọi bất đồng bộ được thực thi.
        Thread.sleep(1000L);
    }

}
