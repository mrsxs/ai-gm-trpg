package com.aigm.common.feign.dto;

import lombok.Data;

/** NPC 对白（GenerateResponse 子项，基线 §6.4）。npcId 必须属当前节点 npcIds 否则丢弃。 */
@Data
public class NpcDialogue {
    private Long npcId;
    private String line;
}
