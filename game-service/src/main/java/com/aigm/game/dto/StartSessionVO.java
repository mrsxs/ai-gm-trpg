package com.aigm.game.dto;

import com.aigm.common.feign.dto.GenerateResponse;
import lombok.Data;

/** 开局返回（基线 §5.3）：{sessionId, state, firstTurn:AiOutput}。 */
@Data
public class StartSessionVO {
    private Long sessionId;
    private GameStateVO state;
    private GenerateResponse firstTurn;
}
