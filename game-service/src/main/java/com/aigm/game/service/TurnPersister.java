package com.aigm.game.service;

import com.aigm.common.exception.BizException;
import com.aigm.common.feign.dto.GenerateResponse;
import com.aigm.common.result.ResultCode;
import com.aigm.game.entity.GameSession;
import com.aigm.game.entity.GameState;
import com.aigm.game.entity.Turn;
import com.aigm.game.mapper.GameSessionMapper;
import com.aigm.game.mapper.GameStateMapper;
import com.aigm.game.mapper.TurnMapper;
import com.aigm.game.util.StateCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 落库（基线 §10，事务内只做本地快速 DB 写）。独立 Bean + REQUIRES_NEW 保证自身事务独立提交，
 * 不被任何外层事务合并。幂等：t_turn 唯一索引 uk_session_turn(session_id, turn_no) 兜底——
 * 并发/重复提交撞唯一键时转 STATE_INVALID(1202)，不暴露 1900。
 */
@Service
@RequiredArgsConstructor
public class TurnPersister {

    private final GameSessionMapper sessionMapper;
    private final GameStateMapper stateMapper;
    private final TurnMapper turnMapper;

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public Turn persist(GameSession session, GameState state, String playerInput,
                        GenerateResponse ai, int newStatus) {
        int turnNo = session.getTurnCount() + 1;
        Turn turn = new Turn();
        turn.setSessionId(session.getId());
        turn.setTurnNo(turnNo);
        turn.setNodeId(state.getCurrentNodeId());
        turn.setPlayerInput(playerInput);
        turn.setAiOutput(StateCodec.toJson(ai));
        try {
            turnMapper.insert(turn); // 先插回合：撞 uk_session_turn 立即回滚，避免脏写 state/session
        } catch (DuplicateKeyException e) {
            throw new BizException(ResultCode.STATE_INVALID, "该回合已提交，请勿重复提交");
        }
        stateMapper.updateById(state);
        session.setTurnCount(turnNo);
        session.setStatus(newStatus);
        sessionMapper.updateById(session);
        return turn;
    }
}
