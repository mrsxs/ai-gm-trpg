package com.aigm.game.dto;

import lombok.Data;

/** 回合返回（基线 §5.3）：{turn, state, finished}。 */
@Data
public class TurnResultVO {
    private TurnVO turn;
    private GameStateVO state;
    private boolean finished;
}
