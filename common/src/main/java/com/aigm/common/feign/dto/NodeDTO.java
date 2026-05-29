package com.aigm.common.feign.dto;

import lombok.Data;

import java.util.List;

/** 当前节点定义（game → ai-engine 的 currentNode，基线 §6.2，含 transitions 白名单）。 */
@Data
public class NodeDTO {
    private Long id;
    private String nodeKey;
    private String title;
    private String narrativeBrief;
    private Boolean isEnding;
    private String endingType;
    private List<Long> npcIds;
    private List<TransitionDTO> transitions;
}
