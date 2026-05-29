package com.aigm.common.feign.dto;

import lombok.Data;

/** 待存记忆条目（基线 §6.4 memoryToStore / §5.5 store items）。importance 1-5。 */
@Data
public class MemoryItem {
    private String content;
    private String memType;
    private Integer importance;
}
