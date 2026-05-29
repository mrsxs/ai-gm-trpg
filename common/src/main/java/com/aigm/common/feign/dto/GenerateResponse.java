package com.aigm.common.feign.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/** ai-engine 结构化输出（基线 §6.4）。validation 为非契约调试字段，下游不得依赖其结构。 */
@Data
public class GenerateResponse {
    private String narrative;
    private List<NpcDialogue> npcDialogues;
    private StateChanges stateChanges;
    private ProposedTransition proposedTransition;
    private List<MemoryItem> memoryToStore;
    private Map<String, Object> validation;
}
