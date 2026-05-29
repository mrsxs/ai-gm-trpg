package com.aigm.scenario.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransitionCreateDTO {
    @NotNull(message = "scenarioId 不能为空")
    private Long scenarioId;
    @NotNull(message = "fromNodeId 不能为空")
    private Long fromNodeId;
    @NotNull(message = "toNodeId 不能为空")
    private Long toNodeId;
    private String conditionExpr;
    private String description;
    private Integer priority;
}
