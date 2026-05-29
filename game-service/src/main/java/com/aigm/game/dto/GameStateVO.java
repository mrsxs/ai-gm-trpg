package com.aigm.game.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/** 运行态状态视图（基线 §6.1）。 */
@Data
public class GameStateVO {
    private Long currentNodeId;
    private Map<String, Boolean> flags;
    private List<String> inventory;
    private Map<String, Integer> attributes;
    private String recentSummary;
}
