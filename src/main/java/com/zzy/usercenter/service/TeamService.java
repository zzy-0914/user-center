package com.zzy.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzy.usercenter.model.domain.Team;
import com.zzy.usercenter.model.domain.User;
import com.zzy.usercenter.model.domain.request.*;

import java.util.List;

/**
* @author ASUS
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2024-03-14 20:55:22
*/
public interface TeamService extends IService<Team> {
    /**
     * 创建队伍
     */
    long addTeam(Team team, User loginUser);



    List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);

    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);


    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    boolean quitTeam(TeamQuitRequest teamQuitRequestRequest, User loginUser);

    boolean deleteTeam(Long id, User loginUser);

}
