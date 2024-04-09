package com.zzy.usercenter.service.impl;

import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class RedissonTest {
    @Resource
    RedissonClient redissonClient;
    @Test
    void test(){
        RList<Object> zzy = redissonClient.getList("zzy");
        zzy.add("牛逼");
        zzy.get(0);

        RMap<Object, Object> zzyMap = redissonClient.getMap("zzyMap");
        zzyMap.put("zzy","sxh");
        Object zzy1 = zzyMap.get("zzy");
        System.out.println(zzy1);
    }
}
