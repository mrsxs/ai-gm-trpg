package com.aigm.common.feign.dto;

import lombok.Data;

/** 出场 NPC（game → ai-engine，knownFacts 已由 game-service 组装，基线 §6.3）。 */
@Data
public class NpcDTO {
    private Long npcId;
    private String npcKey;
    private String name;
    private String persona;
    private String knownFacts;
}
