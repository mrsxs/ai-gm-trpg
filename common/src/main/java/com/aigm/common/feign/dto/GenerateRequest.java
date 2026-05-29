package com.aigm.common.feign.dto;

import lombok.Data;

import java.util.List;

/** game → ai-engine 入参（基线 §6.3）。 */
@Data
public class GenerateRequest {
    private Long sessionId;
    private ScenarioContext scenarioContext;
    private GameStateDTO gameState;
    private NodeDTO currentNode;
    private List<NpcDTO> npcs;
    private List<RecalledMemory> recalledMemories;
    private String playerInput;
    private Boolean isFirstTurn;
}
