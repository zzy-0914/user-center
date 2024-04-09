package com.zzy.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzy.usercenter.model.domain.User;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author ASUS
* @description 针对表【user(用户·)】的数据库操作Service
* @createDate 2024-03-09 23:10:40
*/
public interface UserService extends IService<User> {
    Integer updateUser(User user,User loginUser);

    /**
     * 用户注册
     * @param userAccount 账户
     * @param userPassword 密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount,String userPassword,String checkPassword,String planetCode );

    /**
     * 用户登录
     * @param userAccount 账户
     * @param userPassword 密码
     * @param httpServletRequest
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest httpServletRequest);
    User getSafetyUser(User user);

    int userLogout(HttpServletRequest request);


    List<User> searchUserByTags(List<String> tags);

    User getLoginUser(HttpServletRequest request);

    boolean isAdmin(HttpServletRequest request);

    boolean isAdmin(User user);


    List<User> matchUsers(long num, User user);
}
