package com.aigm.scenario.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScenarioDetailVO {
    private ScenarioVO scenario;
    private List<SceneNodeVO> nodes;
    private List<NpcVO> npcs;
    private List<TransitionVO> transitions;
}
