package com.aigm.common.feign.dto;

import lombok.Data;

import java.util.List;

/** memory/store 入参（基线 §5.5）。 */
@Data
public class StoreRequest {
    private Long sessionId;
    private List<MemoryItem> items;
}
