package com.aigm.scenario.dto;

import lombok.Data;

@Data
public class TransitionUpdateDTO {
    private Long toNodeId;
    private String conditionExpr;
    private String description;
    private Integer priority;
}
