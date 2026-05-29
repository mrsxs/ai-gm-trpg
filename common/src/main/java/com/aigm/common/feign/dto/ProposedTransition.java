package com.aigm.common.feign.dto;

import lombok.Data;

/** AI 提议的跳转（基线 §6.4）。toNodeId 为 null 表示停留当前节点。 */
@Data
public class ProposedTransition {
    private Long toNodeId;
    private String reason;
}
