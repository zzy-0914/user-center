package com.zzy.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serializable;
@Data
public class TeamQuitRequest implements Serializable {
    private static final long serialVersionUID = -2615708110144288706L;
    private Long teamId;
}
