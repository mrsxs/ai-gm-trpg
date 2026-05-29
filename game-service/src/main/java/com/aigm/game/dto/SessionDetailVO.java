package com.aigm.game.dto;

import lombok.Data;

import java.util.List;

/** 读档：state + 全部回合回放（基线 §5.3）。 */
@Data
public class SessionDetailVO {
    private SessionVO session;
    private GameStateVO state;
    private List<TurnVO> turns;
}
