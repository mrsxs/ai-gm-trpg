package com.aigm.user.mapper;

import com.aigm.user.entity.UserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /** 查某用户的角色编码（PLAYER/AUTHOR/ADMIN）。 */
    @Select("SELECT r.role_code FROM t_user_role ur JOIN t_role r ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
    List<String> selectRoleCodesByUserId(Long userId);
}
