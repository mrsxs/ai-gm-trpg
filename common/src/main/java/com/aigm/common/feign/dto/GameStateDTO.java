package com.aigm.common.feign.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/** 运行态状态（基线 §6.1）。flags 缺省 false、attributes 缺省 0。 */
@Data
public class GameStateDTO {
    private Long currentNodeId;
    private Map<String, Boolean> flags;
    private List<String> inventory;
    private Map<String, Integer> attributes;
    private String recentSummary;
}
