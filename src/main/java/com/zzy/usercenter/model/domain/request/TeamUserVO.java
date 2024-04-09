package com.zzy.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
@Data
public class TeamUserVO implements Serializable {


    private static final long serialVersionUID = 2265023852226491752L;
    /**
     *
     * teamId
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建人用户信息
     */
    private UserVO createUser;
    /**
     * 是否加入
     */
    private Boolean hasJoin;
    /**
     * 是否加入
     */
    private Integer hasJoinNum;

}
