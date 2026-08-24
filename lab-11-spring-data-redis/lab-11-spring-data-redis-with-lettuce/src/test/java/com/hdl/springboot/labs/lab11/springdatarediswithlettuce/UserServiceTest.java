package com.hdl.springboot.labs.lab11.springdatarediswithlettuce;

import com.hdl.springboot.labs.lab11.springdatarediswithlettuce.cacheobject.UserCacheObject;
import com.hdl.springboot.labs.lab11.springdatarediswithlettuce.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    @Order(1)
    public void testSet() {
        UserCacheObject object = new UserCacheObject()
                .setId(2)
                .setName("user name 02")
                .setGender(0);
        userService.set(2, object);
    }

    @Test
    @Order(2)
    public void testGet() {
        UserCacheObject object = userService.get(2);
        assertNotNull(object);
        assertEquals(2, object.getId());
    }

}
