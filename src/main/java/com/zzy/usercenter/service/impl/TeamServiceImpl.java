package com.zzy.usercenter.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzy.usercenter.common.ErrorCode;
import com.zzy.usercenter.constant.TeamStatusEnum;
import com.zzy.usercenter.exeception.BusinessException;
import com.zzy.usercenter.model.domain.Team;
import com.zzy.usercenter.model.domain.User;
import com.zzy.usercenter.model.domain.UserTeam;
import com.zzy.usercenter.model.domain.request.*;
import com.zzy.usercenter.service.TeamService;
import com.zzy.usercenter.mapper.TeamMapper;
import com.zzy.usercenter.service.UserService;
import com.zzy.usercenter.service.UserTeamService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
* @author ASUS
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService {
    @Resource
    private UserService userService;
    @Resource
    RedissonClient redissonClient;
    @Resource
    private UserTeamService userTeamService;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(Team team, User loginUser) {
        // 1. 请求参数是否为空？
        if (team==null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        if (loginUser==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        // 2. 是否登录，未登录不允许创建
        int  maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        // 3. 校验信息
        //   1. 队伍人数 > 1 且 <= 20
        if (maxNum<1||maxNum>20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍人数不满足要求");
        }
        final long userId = loginUser.getId();
        //   2. 队伍标题 <= 20
        String name = team.getName();
        if (StringUtils.isBlank(name)||name.length()>20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"标题不符");
        }
        //   3. 描述 <= 512
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description)&&description.length()>512){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"描述不符");
        }
        //   4. status 是否公开（int）不传默认为 0（公开）
        int stats = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(stats);
        if (enumByValue==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"状态错误");
        }
        //   5. 如果 status 是加密状态，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(enumByValue)){
            if (password==null||password.length()>32){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码设置错误");
            }
        }
        // 6. 超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if(new Date().after(expireTime)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间 > 当前时间");
        }
        // 7. 校验用户最多创建 5 个队伍
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.eq("userId",loginUser.getId());
        long count = this.count(teamQueryWrapper);
        if (count>=5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户最多创建 5 个队伍");
        }
        // todo 有 bug，可能同时创建 100 个队伍
        // 8. 插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result || teamId == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"创建队伍失败");
        }

        // 9. 插入用户  => 队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        return teamId;
    }

    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery,boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        // 组合查询条件
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            List<Long> idList = teamQuery.getIdList();
            if (CollectionUtils.isNotEmpty(idList)){
                queryWrapper.in("id",idList);
            }
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).
                        or().like("description", searchText));
            }
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            Integer maxNum = teamQuery.getMaxNum();
            // 查询最大人数相等的
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }
            Long userId = teamQuery.getUserId();
            // 根据创建人来查询
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }
            // 根据状态来查询
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if (!isAdmin){
                if (statusEnum == null) {
                    statusEnum = TeamStatusEnum.PUBLIC;
                }
                if (!isAdmin && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
                    throw new BusinessException(ErrorCode.NO_AUTH);
                }
                queryWrapper.eq("status", statusEnum.getValue());
            }

        }
        // 不展示已过期的队伍
        // expireTime is null or expireTime > now()
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        List<Team> teamList = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(teamList)){
             return new ArrayList<TeamUserVO>();
        }
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        for (Team team :teamList){
            Long userId = team.getUserId();
            if (userId==null){
                continue;
            }
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team,teamUserVO);

            User user = userService.getById(userId);
            // 脱敏用户信息
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }

        return teamUserVOList;
    }

    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if (teamUpdateRequest==null){
            throw  new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamUpdateRequest.getId();
        if (teamId==null&&teamId<=0){
            throw  new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team orderTeam = this.getById(teamId);
        if (orderTeam==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍不存在");
        }
        //只有管理员能修改
        if (!userService.isAdmin(loginUser)&&!orderTeam.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(orderTeam.getStatus());
        if (enumByValue.equals(TeamStatusEnum.SECRET)){
            if (StringUtils.isBlank(teamUpdateRequest.getPassword())){
                throw new BusinessException(ErrorCode.NULL_ERROR,"加盟房间需要设置密码");
            }
        }
        Team newTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest,newTeam);
        boolean result = this.updateById(newTeam);
        return result;
    }

    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamJoinRequest.getTeamId();
        Team teamById = getTeamById(teamId);
        Date expireTime = teamById.getExpireTime();
        if (expireTime!=null && new Date().after(expireTime)){
            throw   new BusinessException(ErrorCode.NULL_ERROR,"队伍过期");
        }
        Integer status = teamById.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }
        String password = teamJoinRequest.getPassword();
        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.SECRET.equals(enumByValue)){
            if (StringUtils.isBlank(password)||!password.equals(teamById.getPassword())){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码错误");
            }
        }
        //该用户加入队伍数量
        Long userId = loginUser.getId();
        //只有一个线程才能获取到锁
        RLock lock = redissonClient.getLock("zzy:join_team");
        while (true){
            try {
                if (lock.tryLock(0,-1, TimeUnit.MILLISECONDS)){
                    QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("userId",userId);
                    long count = userTeamService.count(queryWrapper);
                    if (count>5){
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,"最多加入和创建5个队伍");
                    }
                    //不能重复加入队伍
                    queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("userId",userId);
                    queryWrapper.eq("teamId",teamId);
                    long userHasJoinTeamNum =  userTeamService.count(queryWrapper);
                    if (userHasJoinTeamNum > 0){
                        throw  new BusinessException(ErrorCode.PARAMS_ERROR,"已加入该队伍");
                    }
                    //已加入队伍人数
                    queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("teamId",teamId);
                    long teamHasJoinTeamNum = userTeamService.count(queryWrapper);
                    if (teamHasJoinTeamNum>=teamById.getMaxNum()){
                        throw  new BusinessException(ErrorCode.PARAMS_ERROR,"该队伍人数已满");
                    }
                    //将用户加入队伍
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    boolean result = userTeamService.save(userTeam);
                    return result;

                }
            } catch (InterruptedException e) {
                log.error("doCacheRecommendUser error", e);
                return false;
            } finally {
                // 只能释放自己的锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    @Override
    public boolean quitTeam(TeamQuitRequest teamQuitRequestRequest, User loginUser) {
        if (teamQuitRequestRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamQuitRequestRequest.getTeamId();
        if (teamId==null ||teamId<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //查询用户是否在队伍里
        Team team = getTeamById(teamId);
        Long userId = loginUser.getId();
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",userId);
        queryWrapper.eq("teamId",teamId);
        long count = userTeamService.count(queryWrapper);
        if (count == 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户不在队伍里");
        }
        //队伍剩一人的时候队伍解散
        long teamHasUserNum = countTeamUserByTeam(teamId);//获得当前队伍人数
        if (teamHasUserNum==1){
            this.removeById(teamId);
        }else {
            //队伍至少还有两人
            //判断是否为队长
            if (team.getUserId().equals(userId)){
                QueryWrapper<UserTeam>  hostquerywrapper = new QueryWrapper<>();
                hostquerywrapper.eq("teamId",teamId);
                hostquerywrapper.last("order by id asc limit 2");
                List<UserTeam> userTeamList = userTeamService.list(hostquerywrapper);
                if (CollectionUtils.isEmpty(userTeamList)&&userTeamList.size()<=1){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                UserTeam nextUserTeam = userTeamList.get(1);
                Long nextTeamLeaderId = nextUserTeam.getUserId();
                // 更新当前队伍的队长
                Team updateTeam = new Team();
                updateTeam.setId(teamId);
                updateTeam.setUserId(nextTeamLeaderId);
                boolean result = this.updateById(updateTeam);
                if(!result){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍队长失败");
                }
            }
        }
        return userTeamService.remove(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor =  Exception.class)
    public boolean deleteTeam(Long id, User loginUser) {
        Team team = getTeamById(id);
        if (!team.getUserId().equals(loginUser.getId())){
            throw  new BusinessException(ErrorCode.NULL_ERROR);
        }
        //移除所有相关联的信息 1.Team 2. UserTeam
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId",id);
        boolean result = userTeamService.remove(queryWrapper);
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除失败");
        }
        return this.removeById(id);
    }

    /**
     * 通过teamId获取队伍
     * @param teamId
     * @return
     */
    private Team getTeamById(Long teamId){
        if (teamId==null||teamId<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"队伍不存在");
        }
        return team;
    }

    /**
     * 获取当前队伍人数
     * @param teamId
     * @return
     */
    private long countTeamUserByTeam(Long teamId){
        if (teamId==null||teamId<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId",teamId);
        long count = userTeamService.count(queryWrapper);
        return count;
    }
}




