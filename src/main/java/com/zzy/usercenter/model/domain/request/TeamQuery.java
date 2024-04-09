package com.zzy.usercenter.model.domain.request;


import com.zzy.usercenter.common.PageRequest;
import lombok.Data;

import java.util.List;


/**
 * 查询队伍封装类
 */
@Data
public class TeamQuery extends PageRequest {
    /**
     * id
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
     * 用户id（队长 id）
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 关键词
     */
    private String searchText;
    /**
     * id 列表
     */
    private List<Long> idList;
}
