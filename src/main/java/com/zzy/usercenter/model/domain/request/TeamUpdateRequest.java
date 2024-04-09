package com.zzy.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class TeamUpdateRequest implements Serializable {

    private static final long serialVersionUID = -1761624363595192699L;
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

    private String password;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 关键词
     */
    private String searchText;

}
