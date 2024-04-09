package com.zzy.usercenter.once;
import java.util.Date;

import com.zzy.usercenter.mapper.UserMapper;
import com.zzy.usercenter.model.domain.User;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class InsertUsers {
    @Resource
    UserMapper userMapper;
    // @Scheduled(fixedDelay = 5000,fixedRate = Long.MAX_VALUE)
    public void doInsertUsers(){
        final int Max_DATA_NUMS=1000;
        for (int i = 0; i < Max_DATA_NUMS; i++) {
            User user = new User();
            user.setUsername("sb");
            user.setUserAccount("zzysg");
            user.setAvatarUrl("");
            user.setGender(0);
            user.setUserPassword("88888888");
            user.setPhone("18379262061");
            user.setEmail("3262266039@qq.com");
            user.setUserStatus(0);
            user.setIsDelete(0);
            user.setUserRole(0);
            user.setPlanetCode("");
            userMapper.insert(user);
        }
    }
}
