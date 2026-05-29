package com.aigm.game.mapper;

import com.aigm.game.entity.Turn;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TurnMapper extends BaseMapper<Turn> {

    @Select("SELECT * FROM t_turn WHERE session_id = #{sessionId} ORDER BY turn_no ASC")
    List<Turn> selectBySessionOrderByTurnNo(Long sessionId);
}
