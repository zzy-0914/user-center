package com.zzy.usercenter.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.usercenter.mapper.UserMapper;
import com.zzy.usercenter.model.domain.User;
import com.zzy.usercenter.service.UserService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 开启定时任务，完成缓存预热功能
 */
@Component
public class PreCacheJob {
    @Resource
    private UserService userService;
    @Resource
    RedisTemplate redisTemplate;

    @Resource
    RedissonClient redissonClient;
    //指定预热的用户
    List<Long> userList = Arrays.asList(1L);
    @Scheduled(cron = "0 31 0 * * *")
    public void doCacheRecommendUser(){
        //获取锁对象
        RLock lock = redissonClient.getLock("harem:concubine:recommend:lock");
        try {
            if (lock.tryLock(0,30000L,TimeUnit.MILLISECONDS)){
                //从数据库中查询数据
                for (Long userId : userList) {
                    QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
                    Page<User> page = userService.page(new Page<>(1, 20), userQueryWrapper);
                    //将数据写入到缓存中
                    String redisKey = String.format("harem:concubine:recommend:%s", userId);
                    ValueOperations<String,Object> valueOperations = redisTemplate.opsForValue();
                    try {
                        valueOperations.set(redisKey,page,2, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }finally {
                        //查看当前锁是否是自己
                        if (lock.isHeldByCurrentThread()){
                            lock.unlock();
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }
}
