package com.hdl.springboot.labs.lab11.springdatarediswithlettuce.service;

import com.hdl.springboot.labs.lab11.springdatarediswithlettuce.cacheobject.UserCacheObject;
import com.hdl.springboot.labs.lab11.springdatarediswithlettuce.dao.UserCacheDao;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserCacheDao dao;

    public UserService(UserCacheDao dao) {
        this.dao = dao;
    }

    public UserCacheObject get(Integer id) {
        return dao.get(id);
    }

    public void set(Integer id, UserCacheObject userCacheObject) {
        dao.set(id, userCacheObject);
    }

}
