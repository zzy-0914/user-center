package com.zzy.usercenter.service.impl;

import com.zzy.usercenter.model.domain.User;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

@SpringBootTest
public class RedisTest {
    @Resource
    private RedisTemplate redisTemplate;

    @Test
    void  test(){
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //增
        valueOperations.set("zzyString","dog");
        valueOperations.set("zzyInt",1);
        valueOperations.set("zzyDouble",2.0);
        User user = new User();
        user.setId(1L);
        user.setUserAccount("zzy");
        valueOperations.set("zzyUser",user);
        //查
        Object zzy= valueOperations.get("zzyString");
        Assertions.assertTrue("dog".equals((String) zzy));
        zzy = valueOperations.get("zzyInt");
        Assertions.assertTrue(1 == (Integer) zzy);
        zzy = valueOperations.get("zzyDouble");
        Assertions.assertTrue(2.0 == (Double) zzy);
        System.out.println(valueOperations.get("zzyUser"));
        valueOperations.set("zzyString", "dog");
        redisTemplate.delete("zzyString");

    }
}
