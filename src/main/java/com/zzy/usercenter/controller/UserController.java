package com.zzy.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sun.org.glassfish.gmbal.ParameterNames;
import com.zzy.usercenter.common.BaseResponse;
import com.zzy.usercenter.common.ErrorCode;
import com.zzy.usercenter.common.ResultUtils;
import com.zzy.usercenter.constant.UserConstant;
import com.zzy.usercenter.exeception.BusinessException;
import com.zzy.usercenter.model.domain.User;
import com.zzy.usercenter.model.domain.request.TeamUserVO;
import com.zzy.usercenter.model.domain.request.UserLoginRequest;
import com.zzy.usercenter.model.domain.request.UserRegisterRequest;
import com.zzy.usercenter.model.domain.request.UserVO;
import com.zzy.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zzy.usercenter.constant.UserConstant.USER_LOGIN_STATE;
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTgs(@RequestParam(required = false) List<String> tagNameList){
        if (CollectionUtils.isEmpty(tagNameList)){
            throw new  BusinessException(ErrorCode.NULL_ERROR,"参数为空");
        }
        List<User> users = userService.searchUserByTags(tagNameList);
        return ResultUtils.success(users);
    }

    @PostMapping(value = "/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest){
        if (userRegisterRequest ==null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        String userPassword = userRegisterRequest.getUserPassword();
        String userAccount = userRegisterRequest.getUserAccount();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        long result = userService.userRegister(userAccount,
                userPassword, checkPassword, planetCode);
        return ResultUtils.success(result);
        }

    @PostMapping(value = "/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        if (userLoginRequest ==null){
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        String userPassword = userLoginRequest.getUserPassword();
        String userAccount = userLoginRequest.getUserAccount();
        if (StringUtils.isAnyBlank(userAccount,userPassword)){
            return ResultUtils.error(ErrorCode.NULL_ERROR)
            ;
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "缺少管理员权限");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }
    @GetMapping("/delete")
    public BaseResponse<Boolean>  searchUsers(@RequestBody long id,HttpServletRequest request){
        //判断是否是管理员
        if (userService.isAdmin(request)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (id<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(id);
       return ResultUtils.success(b);
    }
    @PostMapping("/logout")
    public BaseResponse<Integer>  userLogout(HttpServletRequest request){
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long userId = currentUser.getId();
        // TODO 校验用户是否合法
        User user = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }

    /**
     * 对用户提交的信息进行更新
     * @return
     */
    @RequestMapping("/update")
    public BaseResponse<Integer> updateDate(@RequestBody User user,HttpServletRequest request){
        //1.校验参数是否为空
        if (user==null){
            throw new  BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2.对用户进行校验 一.管理员可以对所有用户进行就该 二 判断是否是其本人在操作
        User loginUser = userService.getLoginUser(request);
        Integer result = userService.updateUser(user, loginUser);
        return ResultUtils.success(result);

    }

    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(int pageSize,int pageNum,  HttpServletRequest request) {
        //获取当前用户
        User loginUser = userService.getLoginUser(request);
        String redisKey = String.format("harem:concubine:recommend:%s", loginUser.getId());
        //如果有缓存，直接读缓存
        Page<User> userPage = (Page<User>) redisTemplate.opsForValue().get(redisKey);
        if (userPage!=null){
            return ResultUtils.success(userPage);
        }
        //无缓存，查数据
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        Page<User> page = userService.page(new Page<>(pageNum,pageSize), userQueryWrapper);
        //将查询到的数据写入到redis缓存值中
        redisTemplate.opsForValue().set(redisKey,page,1,TimeUnit.MINUTES);
        return ResultUtils.success(page);
    }
    /**
     * 获取匹配的用户
     */
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest request) {
        if (num <= 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.matchUsers(num, user));
    }

}
