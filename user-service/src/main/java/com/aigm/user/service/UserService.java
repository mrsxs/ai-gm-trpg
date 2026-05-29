package com.aigm.user.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.aigm.common.constant.RoleConst;
import com.aigm.common.exception.BizException;
import com.aigm.common.result.ResultCode;
import com.aigm.common.util.PageQuery;
import com.aigm.common.util.PageResult;
import com.aigm.user.dto.*;
import com.aigm.user.entity.Role;
import com.aigm.user.entity.User;
import com.aigm.user.entity.UserRole;
import com.aigm.user.mapper.RoleMapper;
import com.aigm.user.mapper.UserMapper;
import com.aigm.user.mapper.UserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long register(RegisterDTO dto) {
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BizException(ResultCode.RESOURCE_EXISTS, "用户名已存在");
        }
        User u = new User();
        u.setUsername(dto.getUsername());
        u.setPassword(passwordEncoder.encode(dto.getPassword()));
        u.setNickname(dto.getNickname() == null || dto.getNickname().isBlank()
                ? dto.getUsername() : dto.getNickname());
        u.setStatus(1);
        userMapper.insert(u);
        // 默认授予 PLAYER
        Role player = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, RoleConst.PLAYER));
        UserRole ur = new UserRole();
        ur.setUserId(u.getId());
        ur.setRoleId(player.getId());
        userRoleMapper.insert(ur);
        return u.getId();
    }

    public LoginVO login(LoginDTO dto) {
        User u = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (u == null || !passwordEncoder.matches(dto.getPassword(), u.getPassword())) {
            throw new BizException(ResultCode.AUTH_BAD_CREDENTIALS);
        }
        if (u.getStatus() != null && u.getStatus() == 0) {
            throw new BizException(ResultCode.AUTH_ACCOUNT_DISABLED);
        }
        List<String> roles = userRoleMapper.selectRoleCodesByUserId(u.getId());
        // sa-token 签发，会话存 roles/username 供网关读取并下发 X-User-* 头
        StpUtil.login(u.getId());
        SaSession session = StpUtil.getSession();
        session.set("roles", roles);
        session.set("username", u.getUsername());

        LoginVO vo = new LoginVO();
        vo.setToken(StpUtil.getTokenValue());
        vo.setUserInfo(toVO(u, roles));
        return vo;
    }

    public UserVO currentUser(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        return toVO(u, userRoleMapper.selectRoleCodesByUserId(userId));
    }

    public void updateProfile(Long userId, UpdateProfileDTO dto) {
        User u = new User();
        u.setId(userId);
        u.setNickname(dto.getNickname());
        u.setAvatar(dto.getAvatar());
        userMapper.updateById(u);
    }

    public PageResult<UserVO> pageUsers(PageQuery pq, String username) {
        Page<User> page = new Page<>(pq.getPage(), pq.getSize());
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            qw.like(User::getUsername, username);
        }
        qw.orderByDesc(User::getId);
        Page<User> result = userMapper.selectPage(page, qw);
        List<UserVO> list = new ArrayList<>();
        for (User u : result.getRecords()) {
            list.add(toVO(u, userRoleMapper.selectRoleCodesByUserId(u.getId())));
        }
        return PageResult.of(list, result.getTotal(), pq.getPage(), pq.getSize());
    }

    public void updateStatus(Long id, Integer status) {
        User u = new User();
        u.setId(id);
        u.setStatus(status);
        userMapper.updateById(u);
    }

    @Transactional
    public void assignRoles(Long userId, List<String> roleCodes) {
        if (userMapper.selectById(userId) == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        List<Role> roles = roleMapper.selectList(
                new LambdaQueryWrapper<Role>().in(Role::getRoleCode, roleCodes));
        if (roles.size() != roleCodes.stream().distinct().count()) {
            throw new BizException(ResultCode.PARAM_INVALID, "包含非法角色编码");
        }
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        for (Role r : roles) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(r.getId());
            userRoleMapper.insert(ur);
        }
    }

    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }

    private UserVO toVO(User u, List<String> roles) {
        UserVO vo = new UserVO();
        vo.setUserId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setNickname(u.getNickname());
        vo.setAvatar(u.getAvatar());
        vo.setStatus(u.getStatus());
        vo.setRoles(roles);
        vo.setCreatedAt(u.getCreatedAt());
        return vo;
    }
}
