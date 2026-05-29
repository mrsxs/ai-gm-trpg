package com.aigm.game.service;

import com.aigm.common.feign.dto.GenerateResponse;
import com.aigm.game.entity.GameSession;
import com.aigm.game.entity.GameState;
import com.aigm.game.entity.Turn;
import com.aigm.game.mapper.GameSessionMapper;
import com.aigm.game.mapper.GameStateMapper;
import com.aigm.game.mapper.TurnMapper;
import com.aigm.game.util.StateCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 落库（基线 §10，事务内只做本地快速 DB 写）。独立 Bean 以保证 @Transactional 经代理生效。
 * 幂等：t_turn 唯一索引 uk_session_turn(session_id, turn_no) 兜底。
 */
@Service
@RequiredArgsConstructor
public class TurnPersister {

    private final GameSessionMapper sessionMapper;
    private final GameStateMapper stateMapper;
    private final TurnMapper turnMapper;

    @Transactional(rollbackFor = Exception.class)
    public Turn persist(GameSession session, GameState state, String playerInput,
                        GenerateResponse ai, int newStatus) {
        stateMapper.updateById(state);

        int turnNo = session.getTurnCount() + 1;
        Turn turn = new Turn();
        turn.setSessionId(session.getId());
        turn.setTurnNo(turnNo);
        turn.setNodeId(state.getCurrentNodeId());
        turn.setPlayerInput(playerInput);
        turn.setAiOutput(StateCodec.toJson(ai));
        turnMapper.insert(turn);

        session.setTurnCount(turnNo);
        session.setStatus(newStatus);
        sessionMapper.updateById(session);
        return turn;
    }
}
