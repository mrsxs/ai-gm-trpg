package com.aigm.common.feign.dto;

import lombok.Data;

/** 状态机分支（白名单条目，基线 §6.2）。condition 用 §6.2.1 受控小语法。 */
@Data
public class TransitionDTO {
    private Long toNodeId;
    private String condition;
    private Integer priority;
    private String description;
}
