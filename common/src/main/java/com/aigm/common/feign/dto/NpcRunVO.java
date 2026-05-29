package com.aigm.common.feign.dto;

import lombok.Data;

/**
 * 运行时 NPC 原料（scenario → game，基线 §6.3 注）。
 * scenario 只给原料(persona/background/secret)，knownFacts 由 game-service 按 flag 动态组装。
 */
@Data
public class NpcRunVO {
    private Long npcId;
    private String npcKey;
    private String name;
    private String persona;
    private String background;
    private String secret;
}
