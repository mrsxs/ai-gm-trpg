package com.aigm.game.mapper;

import com.aigm.game.entity.GameState;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GameStateMapper extends BaseMapper<GameState> {

    @Select("SELECT * FROM t_game_state WHERE session_id = #{sessionId}")
    GameState selectBySessionId(Long sessionId);
}
