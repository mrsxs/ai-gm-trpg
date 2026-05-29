package com.aigm.scenario.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_scenario")
public class Scenario {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String intro;
    private String cover;
    private String genre;
    private Long startNodeId;
    private Integer status;
    private Long authorId;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
