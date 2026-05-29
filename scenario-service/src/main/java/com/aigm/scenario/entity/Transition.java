package com.aigm.scenario.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_transition")
public class Transition {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scenarioId;
    private Long fromNodeId;
    private Long toNodeId;
    private String conditionExpr;
    private String description;
    private Integer priority;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
