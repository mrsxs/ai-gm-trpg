package com.aigm.scenario.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TransitionVO {
    private Long id;
    private Long scenarioId;
    private Long fromNodeId;
    private Long toNodeId;
    private String conditionExpr;
    private String description;
    private Integer priority;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
