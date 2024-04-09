package com.zzy.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.usercenter.common.BaseResponse;
import com.zzy.usercenter.common.ErrorCode;
import com.zzy.usercenter.common.ResultUtils;
import com.zzy.usercenter.model.domain.UserTeam;
import com.zzy.usercenter.model.domain.request.*;
import com.zzy.usercenter.exeception.BusinessException;
import com.zzy.usercenter.model.domain.Team;
import com.zzy.usercenter.model.domain.User;
import com.zzy.usercenter.service.TeamService;
import com.zzy.usercenter.service.UserService;
import com.zzy.usercenter.service.UserTeamService;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/team")
@CrossOrigin(origins = {"http://localhost:3000"})
public class TeamController {
    @Resource
    private UserService userService;
    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;
    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest,
                                      HttpServletRequest request){
         if (teamAddRequest == null){
             throw new BusinessException(ErrorCode.NULL_ERROR);
         }
        User loginUser = userService.getLoginUser(request);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest,team);
        long teamId = teamService.addTeam(team, loginUser);
        return ResultUtils.success(teamId);
    }


    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest,
                                            HttpServletRequest request){
        if (teamUpdateRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser);

        return ResultUtils.success(result);
    }
    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> listTeams(TeamQuery teamQuery,HttpServletRequest request){
        if (teamQuery==null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        boolean isAdmin = userService.isAdmin(request);
        List<TeamUserVO> teamUserVOList = teamService.listTeams(teamQuery,isAdmin);
        User loginUser = userService.getLoginUser(request);
        teamUserVOList = getTeamUserVOList(teamUserVOList, loginUser);
        return ResultUtils.success(teamUserVOList);
    }


    @PostMapping("/list/age")
    public BaseResponse<Page<Team> > listTeamsByPage(TeamQuery teamQuery){
        if (teamQuery==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(team,teamQuery);
        Page<Team> teamPage = new Page<Team>(teamQuery.getPageNum(), teamQuery.getPageSize());
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>(team);
        Page<Team> resultPage = teamService.page(teamPage, teamQueryWrapper);
        return ResultUtils.success(resultPage);
    }
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest,HttpServletRequest request){
        if (teamJoinRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(result);
    }
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequestRequest,HttpServletRequest request){
        if (teamQuitRequestRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.quitTeam(teamQuitRequestRequest, loginUser);
        return ResultUtils.success(result);
    }
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest,HttpServletRequest request){
        if (deleteRequest == null&& deleteRequest.getId()<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.deleteTeam(id,loginUser);
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除失败");
        }
        return ResultUtils.success(true);
    }
    /**
     * 获取我创建的队伍
     * */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyCreateTeams(TeamQuery teamQuery,HttpServletRequest request){
        if (teamQuery==null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        teamQuery.setUserId(loginUser.getId());
        List<TeamUserVO> teamUserVOList = teamService.listTeams(teamQuery,true);

        return ResultUtils.success(teamUserVOList);
    }
    /**
     * 获取我加入的队伍
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeams(TeamQuery teamQuery,HttpServletRequest request){
        if (teamQuery==null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",loginUser.getId());
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        if (CollectionUtils.isEmpty(userTeamList)){
            return ResultUtils.success(new ArrayList<>());
        }
        // 取出不重复的队伍 teamId(teamId 一版不会有重复情况）
        // teamId userId
        // 1, 2
        // 1, 3
        // 2, 3
        // result
        // 1 => 2, 3
        // 2 => 3
        Map<Long,List<UserTeam>> listMap = userTeamList.stream().
                collect((Collectors.groupingBy(UserTeam::getTeamId)));
        List<Long> idList = new ArrayList<>(listMap.keySet());
        teamQuery.setIdList(idList);
        List<TeamUserVO> teamUserVOList = teamService.listTeams(teamQuery,true);
        //获取到id集合
        teamUserVOList = getTeamUserVOList(teamUserVOList, loginUser);
        return ResultUtils.success(teamUserVOList);
    }

    private List<TeamUserVO> getTeamUserVOList(List<TeamUserVO> teamUserVOList,User loginUser){
        //获取到id集合
        List<Long> teamIdList = teamUserVOList.stream().map
                (TeamUserVO::getId).collect(Collectors.toList());
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",loginUser.getId());
        queryWrapper.in("teamId",teamIdList);
        List<UserTeam> list = userTeamService.list(queryWrapper);
        Set<Long> collect = list.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
        teamUserVOList.forEach(team->{
            boolean hasJoin = collect.contains(team.getId());
            team.setHasJoin(hasJoin);
        });
        //查询已加入队伍的人数
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.in("teamId",teamIdList);
        List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
        Map<Long, List<UserTeam>> teamIdUserTeamList =
                userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        teamUserVOList.forEach(teamUserVO -> teamUserVO.setHasJoinNum(teamIdUserTeamList.getOrDefault(teamUserVO.getId(),new ArrayList<>()).size()));
        return teamUserVOList;
    }
}
