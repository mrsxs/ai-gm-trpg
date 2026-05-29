package com.aigm.common.feign.dto;

import lombok.Data;

import java.util.List;

/** 运行时节点完整定义（scenario → game，含出场 NPC 原料与 transitions 白名单，基线 §5.2/§6.2）。 */
@Data
public class SceneNodeRunVO {
    private Long id;
    private String nodeKey;
    private String title;
    private String narrativeBrief;
    private Boolean isEnding;
    private String endingType;
    private List<NpcRunVO> npcs;
    private List<TransitionDTO> transitions;
}
